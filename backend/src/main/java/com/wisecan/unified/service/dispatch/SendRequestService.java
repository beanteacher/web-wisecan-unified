package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.dispatch.*;
import com.wisecan.unified.domain.dispatch.encoding.SmsEncoding;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import com.wisecan.unified.dto.dispatch.SendRequestDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 발송 요청 적재 서비스 (W-203).
 *
 * <p>책임 범위 — 05_DATA_MODEL.md §5.4 "우리 책임 범위" 3단계 중 1단계:</p>
 * <ol>
 *   <li>검증 게이트 실행 ({@link SendValidationService} 위임 — W-201)</li>
 *   <li>SMS 인코딩·타입 분기 ({@link SmsEncoding} 위임 — W-202)</li>
 *   <li>{@link SendRequest} 엔티티 적재 (본 클래스 핵심 책임)</li>
 * </ol>
 *
 * <p>W-205: ApiKey 조회 후 keyType을 SendValidationContext에 포함시켜
 * NetworkRoutingGate가 테스트/상용 망 분리를 검증하게 한다.
 * 요청 헤더 {@code X-Network-Type}으로 망을 명시하며, 미지정 시 키 유형에서 자동 결정한다.</p>
 *
 * <p>외부 발송 시스템 INSERT(2단계)와 잔액 차감(§7)은 W-204에서 구현한다.
 * 현재는 적재 후 status=PENDING을 반환한다.</p>
 *
 * <p>적재 P95 ≤ 1s DoD: 단일 트랜잭션 내 검증+엔티티 저장이며,
 * 외부 시스템 I/O(W-204)가 포함되지 않는 이 단계에서는 DB INSERT 단건 기준 달성 가능.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendRequestService {

    private final SendValidationService sendValidationService;
    private final SendRequestRepository sendRequestRepository;
    private final ApiKeyRepository apiKeyRepository;

    // ── 단건 발송 ─────────────────────────────────────────────────────

    /**
     * 단건 발송 요청을 적재한다.
     *
     * @param memberId    발송 요청자 회원 ID
     * @param apiKeyId    인증에 사용된 API Key ID
     * @param unitCost    건당 단가 (원화)
     * @param networkType 요청된 발송 망 (null이면 키 유형에서 자동 결정)
     * @param request     단건 발송 요청 DTO
     * @return 적재 완료 응답 (send_id ULID 포함)
     */
    @Transactional(rollbackFor = Exception.class)
    public SendRequestDto.AcceptResponse sendSingle(
            Long memberId,
            Long apiKeyId,
            long unitCost,
            NetworkType networkType,
            SendRequestDto.SingleRequest request
    ) {
        // 0) API Key 조회 — keyType 망 분리 판별용 (W-205)
        ApiKey apiKey = loadApiKey(apiKeyId);
        NetworkType resolvedNetwork = resolveNetwork(apiKey, networkType);

        // 1) 검증 게이트 실행 (W-201, W-205 NetworkRoutingGate 포함)
        SendValidationContext ctx = buildValidationContext(
                memberId, apiKeyId, apiKey, request.callbackNumber(), request.channel(),
                request.messageBody(), request.isAdvertisement(), 1, unitCost, resolvedNetwork
        );
        sendValidationService.validate(ctx);

        // 2) SMS 계열 인코딩·타입 분기 (W-202)
        SmsMessageType smsType = resolveSmsType(request.channel(), request.messageBody());

        // 3) 엔티티 적재
        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .channel(request.channel())
                .smsType(smsType)
                .callbackNumber(request.callbackNumber())
                .recipientNumbers(request.recipientNumber())
                .recipientCount(1)
                .subject(request.subject())
                .messageBody(request.messageBody())
                .isAdvertisement(request.isAdvertisement())
                .senderKey(request.senderKey())
                .templateCode(request.templateCode())
                .requestedAt(request.scheduledAt() != null ? request.scheduledAt() : LocalDateTime.now())
                .groupId(null)
                .routingMeta(null) // W-204에서 라우팅 메타 산출 후 업데이트
                .unitCost(unitCost)
                .build();

        SendRequest saved = sendRequestRepository.save(entity);

        log.info("[SendRequest] 단건 적재 완료 — sendId={}, memberId={}, channel={}, network={}, recipientCount=1",
                saved.getSendId(), memberId, request.channel(), resolvedNetwork);

        return SendRequestDto.AcceptResponse.from(saved);
    }

    /**
     * 단건 발송 요청을 적재한다 (networkType 미지정 — 하위 호환).
     */
    @Transactional(rollbackFor = Exception.class)
    public SendRequestDto.AcceptResponse sendSingle(
            Long memberId,
            Long apiKeyId,
            long unitCost,
            SendRequestDto.SingleRequest request
    ) {
        return sendSingle(memberId, apiKeyId, unitCost, null, request);
    }

    // ── 다건 발송 ─────────────────────────────────────────────────────

    /**
     * 다건(일괄) 발송 요청을 적재한다.
     *
     * <p>동일 {@code groupId}로 묶어 외부 시스템 group_id에 매핑한다.
     * 수신자 N명 각각 별도 엔티티로 적재하지 않고,
     * 단일 엔티티에 수신자 목록을 쉼표 구분 문자열로 저장한다.
     * (05_DATA_MODEL.md §5.5: 일괄 발송은 동일 group_id 공유)</p>
     *
     * @param memberId    발송 요청자 회원 ID
     * @param apiKeyId    인증에 사용된 API Key ID
     * @param unitCost    건당 단가 (원화)
     * @param networkType 요청된 발송 망 (null이면 키 유형에서 자동 결정)
     * @param request     다건 발송 요청 DTO
     * @return 적재 완료 응답 (send_id ULID 포함)
     */
    @Transactional(rollbackFor = Exception.class)
    public SendRequestDto.AcceptResponse sendBulk(
            Long memberId,
            Long apiKeyId,
            long unitCost,
            NetworkType networkType,
            SendRequestDto.BulkRequest request
    ) {
        List<String> recipients = request.recipientNumbers();
        int recipientCount = recipients.size();

        // 0) API Key 조회 — keyType 망 분리 판별용 (W-205)
        ApiKey apiKey = loadApiKey(apiKeyId);
        NetworkType resolvedNetwork = resolveNetwork(apiKey, networkType);

        // 1) 검증 게이트 실행 (W-201, W-205 NetworkRoutingGate 포함)
        SendValidationContext ctx = buildValidationContext(
                memberId, apiKeyId, apiKey, request.callbackNumber(), request.channel(),
                request.messageBody(), request.isAdvertisement(), recipientCount, unitCost, resolvedNetwork
        );
        sendValidationService.validate(ctx);

        // 2) SMS 계열 인코딩·타입 분기 (W-202)
        SmsMessageType smsType = resolveSmsType(request.channel(), request.messageBody());

        // 3) 그룹 ID 생성 (일괄 발송 묶음 식별 — 외부 시스템 group_id 매핑용)
        long groupId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        // 4) 엔티티 적재
        String recipientNumbersCsv = String.join(",", recipients);
        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .channel(request.channel())
                .smsType(smsType)
                .callbackNumber(request.callbackNumber())
                .recipientNumbers(recipientNumbersCsv)
                .recipientCount(recipientCount)
                .subject(request.subject())
                .messageBody(request.messageBody())
                .isAdvertisement(request.isAdvertisement())
                .senderKey(request.senderKey())
                .templateCode(request.templateCode())
                .requestedAt(request.scheduledAt() != null ? request.scheduledAt() : LocalDateTime.now())
                .groupId(groupId)
                .routingMeta(null) // W-204에서 라우팅 메타 산출 후 업데이트
                .unitCost(unitCost)
                .build();

        SendRequest saved = sendRequestRepository.save(entity);

        log.info("[SendRequest] 다건 적재 완료 — sendId={}, memberId={}, channel={}, network={}, recipientCount={}, groupId={}",
                saved.getSendId(), memberId, request.channel(), resolvedNetwork, recipientCount, groupId);

        return SendRequestDto.AcceptResponse.from(saved);
    }

    /**
     * 다건 발송 요청을 적재한다 (networkType 미지정 — 하위 호환).
     */
    @Transactional(rollbackFor = Exception.class)
    public SendRequestDto.AcceptResponse sendBulk(
            Long memberId,
            Long apiKeyId,
            long unitCost,
            SendRequestDto.BulkRequest request
    ) {
        return sendBulk(memberId, apiKeyId, unitCost, null, request);
    }

    // ── 조회 ─────────────────────────────────────────────────────────

    /**
     * send_id(ULID)로 발송 요청 상세를 조회한다.
     *
     * @param sendId ULID 26자
     * @return 발송 요청 상세 응답 (라우팅 메타 비포함)
     */
    @Transactional(readOnly = true)
    public SendRequestDto.DetailResponse getDetail(String sendId) {
        SendRequest entity = sendRequestRepository.findBySendId(sendId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "발송 요청을 찾을 수 없습니다: sendId=" + sendId));
        return SendRequestDto.DetailResponse.from(entity);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────

    /** API Key를 로드한다. 없으면 EntityNotFoundException. */
    private ApiKey loadApiKey(Long apiKeyId) {
        return apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new EntityNotFoundException("API Key를 찾을 수 없습니다: id=" + apiKeyId));
    }

    /**
     * 요청된 networkType이 null이면 API Key 유형에서 자동 결정한다.
     * API Key가 TEST면 TEST망, PRODUCTION이면 PRODUCTION망.
     */
    private NetworkType resolveNetwork(ApiKey apiKey, NetworkType requested) {
        if (requested != null) {
            return requested;
        }
        return switch (apiKey.getKeyType()) {
            case TEST       -> NetworkType.TEST;
            case PRODUCTION -> NetworkType.PRODUCTION;
        };
    }

    private SendValidationContext buildValidationContext(
            Long memberId, Long apiKeyId, ApiKey apiKey,
            String callbackNumber, SendChannel channel,
            String messageBody, boolean isAdvertisement,
            int recipientCount, long unitCost, NetworkType networkType
    ) {
        return new SendValidationContext(
                memberId,
                apiKeyId,
                apiKey.getKeyType(),
                callbackNumber,
                channel,
                messageBody,
                isAdvertisement,
                recipientCount,
                unitCost,
                networkType,
                null
        );
    }

    /**
     * SMS 계열 채널의 메시지 타입을 분기한다.
     * 카카오·RCS는 SMS 인코딩 불필요 — null 반환.
     */
    private SmsMessageType resolveSmsType(SendChannel channel, String messageBody) {
        return switch (channel) {
            case SMS, LMS, MMS -> SmsEncoding.resolveType(messageBody);
            case KAKAO, RCS -> null;
        };
    }
}
