package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.*;
import com.wisecan.unified.domain.dispatch.encoding.SmsEncoding;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import com.wisecan.unified.dto.dispatch.WebSendDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 웹 콘솔 발송 서비스 (W-206).
 *
 * <p>JWT 인증 기반 회원 세션에서 호출되는 발송 서비스.
 * API Key 기반 {@link SendRequestService}와 분리하여 웹 콘솔 고유 흐름을 관리한다.</p>
 *
 * <p>책임 범위 (02_FEATURE_SPEC.md §6):</p>
 * <ul>
 *   <li>§6.1 단건 발송 — 5채널, 수신번호 최대 1,000개</li>
 *   <li>§6.2 일괄 발송 — CSV 파싱 결과 최대 100,000행</li>
 *   <li>§6.3 예약 발송 / 취소</li>
 * </ul>
 *
 * <p>웹 콘솔은 회원의 대표 API Key(PRODUCTION, ACTIVE, 최신 발급 순)를 자동 선택해
 * 발송 적재에 사용한다. 테스트 키 전용 테스트 발송은 별도 지원하지 않는다.</p>
 *
 * <p>외부 발송 시스템 INSERT(W-204) 및 잔액 차감은 {@link ExternalDispatchService}가 담당한다.
 * 본 서비스는 적재(PENDING) 후 ACCEPTED 응답만 반환한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSendService {

    private final SendValidationService sendValidationService;
    private final SendRequestRepository sendRequestRepository;
    private final ApiKeyRepository apiKeyRepository;

    // 단가 임시 고정값 — 채널별 단가 테이블 연동 전 기본값 (원화)
    private static final long DEFAULT_UNIT_COST = 20L;

    // ── 단건 발송 ─────────────────────────────────────────────────────

    /**
     * 웹 콘솔 단건 발송을 적재한다.
     *
     * <p>수신번호 목록을 단일 엔티티에 쉼표 구분 문자열로 적재한다.
     * 즉시 발송은 requestedAt = 현재 시각으로 설정된다.</p>
     *
     * @param memberId 로그인 회원 ID (JWT에서 추출)
     * @param request  단건 발송 요청 DTO
     * @return 적재 완료 응답
     */
    @Transactional(rollbackFor = Exception.class)
    public WebSendDto.AcceptResponse send(Long memberId, WebSendDto.SingleRequest request) {
        ApiKey apiKey = resolveProductionApiKey(memberId);

        SendValidationContext ctx = buildContext(
                memberId, apiKey,
                request.callbackNumber(), request.channel(),
                request.messageBody(), request.isAdvertisement(),
                request.recipientNumbers().size(), DEFAULT_UNIT_COST
        );
        sendValidationService.validate(ctx);

        SmsMessageType smsType = resolveSmsType(request.channel(), request.messageBody());
        String recipientsCsv = String.join(",", request.recipientNumbers());

        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKey.getId())
                .channel(request.channel())
                .smsType(smsType)
                .callbackNumber(request.callbackNumber())
                .recipientNumbers(recipientsCsv)
                .recipientCount(request.recipientNumbers().size())
                .subject(request.subject())
                .messageBody(request.messageBody())
                .isAdvertisement(request.isAdvertisement())
                .senderKey(request.senderKey())
                .templateCode(request.templateCode())
                .requestedAt(LocalDateTime.now())
                .groupId(null)
                .routingMeta(null)
                .unitCost(DEFAULT_UNIT_COST)
                .build();

        SendRequest saved = sendRequestRepository.save(entity);

        log.info("[WebSend] 단건 적재 완료 — sendId={}, memberId={}, channel={}, recipientCount={}",
                saved.getSendId(), memberId, request.channel(), saved.getRecipientCount());

        return WebSendDto.AcceptResponse.from(saved);
    }

    // ── 일괄 발송 ─────────────────────────────────────────────────────

    /**
     * 웹 콘솔 일괄 발송을 적재한다.
     *
     * <p>수신자 최대 100,000행을 단일 엔티티에 그룹 ID로 묶어 적재한다 (02 §6.2).
     * 부분 발송/취소 분기는 {@code HTTP 207} 응답으로 처리하며,
     * 현재는 전체 적재 성공 응답만 반환한다.</p>
     *
     * @param memberId 로그인 회원 ID
     * @param request  일괄 발송 요청 DTO
     * @return 적재 완료 응답
     */
    @Transactional(rollbackFor = Exception.class)
    public WebSendDto.AcceptResponse sendBulk(Long memberId, WebSendDto.BulkRequest request) {
        List<String> recipients = request.recipientNumbers();
        ApiKey apiKey = resolveProductionApiKey(memberId);

        SendValidationContext ctx = buildContext(
                memberId, apiKey,
                request.callbackNumber(), request.channel(),
                request.messageBody(), request.isAdvertisement(),
                recipients.size(), DEFAULT_UNIT_COST
        );
        sendValidationService.validate(ctx);

        SmsMessageType smsType = resolveSmsType(request.channel(), request.messageBody());
        long groupId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String recipientsCsv = String.join(",", recipients);

        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKey.getId())
                .channel(request.channel())
                .smsType(smsType)
                .callbackNumber(request.callbackNumber())
                .recipientNumbers(recipientsCsv)
                .recipientCount(recipients.size())
                .subject(request.subject())
                .messageBody(request.messageBody())
                .isAdvertisement(request.isAdvertisement())
                .senderKey(request.senderKey())
                .templateCode(request.templateCode())
                .requestedAt(LocalDateTime.now())
                .groupId(groupId)
                .routingMeta(null)
                .unitCost(DEFAULT_UNIT_COST)
                .build();

        SendRequest saved = sendRequestRepository.save(entity);

        log.info("[WebSend] 일괄 적재 완료 — sendId={}, memberId={}, channel={}, recipientCount={}, groupId={}",
                saved.getSendId(), memberId, request.channel(), recipients.size(), groupId);

        return WebSendDto.AcceptResponse.from(saved);
    }

    // ── 예약 발송 ─────────────────────────────────────────────────────

    /**
     * 웹 콘솔 예약 발송을 적재한다.
     *
     * <p>발송 시각은 미래 시각만 허용한다 (DTO {@code @Future} 검증).
     * 적재 → 실제 송출 시각 편차 ≤ 60초 NFR (02 §6.3).</p>
     *
     * @param memberId 로그인 회원 ID
     * @param request  예약 발송 요청 DTO
     * @return 적재 완료 응답 (scheduledAt 포함)
     */
    @Transactional(rollbackFor = Exception.class)
    public WebSendDto.AcceptResponse sendScheduled(Long memberId, WebSendDto.ScheduledRequest request) {
        ApiKey apiKey = resolveProductionApiKey(memberId);

        SendValidationContext ctx = buildContext(
                memberId, apiKey,
                request.callbackNumber(), request.channel(),
                request.messageBody(), request.isAdvertisement(),
                request.recipientNumbers().size(), DEFAULT_UNIT_COST
        );
        sendValidationService.validate(ctx);

        SmsMessageType smsType = resolveSmsType(request.channel(), request.messageBody());
        String recipientsCsv = String.join(",", request.recipientNumbers());

        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKey.getId())
                .channel(request.channel())
                .smsType(smsType)
                .callbackNumber(request.callbackNumber())
                .recipientNumbers(recipientsCsv)
                .recipientCount(request.recipientNumbers().size())
                .subject(request.subject())
                .messageBody(request.messageBody())
                .isAdvertisement(request.isAdvertisement())
                .senderKey(request.senderKey())
                .templateCode(request.templateCode())
                .requestedAt(request.scheduledAt())
                .groupId(null)
                .routingMeta(null)
                .unitCost(DEFAULT_UNIT_COST)
                .build();

        SendRequest saved = sendRequestRepository.save(entity);

        log.info("[WebSend] 예약 적재 완료 — sendId={}, memberId={}, channel={}, scheduledAt={}",
                saved.getSendId(), memberId, request.channel(), request.scheduledAt());

        return WebSendDto.AcceptResponse.from(saved);
    }

    // ── 예약 취소 ─────────────────────────────────────────────────────

    /**
     * 예약 발송을 취소한다.
     *
     * <p>발송 시각 이전 PENDING 상태 엔티티만 취소 가능하다 (02 §6.3).
     * 이미 QUEUED(외부 시스템 INSERT 완료) 이후 상태는 취소할 수 없다.</p>
     *
     * @param memberId 로그인 회원 ID
     * @param sendId   취소할 발송 요청 ULID
     * @param reason   취소 사유 (선택)
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelScheduled(Long memberId, String sendId, String reason) {
        SendRequest entity = sendRequestRepository.findBySendId(sendId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "발송 요청을 찾을 수 없습니다: sendId=" + sendId));

        if (!entity.getMemberId().equals(memberId)) {
            throw new EntityNotFoundException("발송 요청을 찾을 수 없습니다: sendId=" + sendId);
        }

        if (entity.getStatus() != SendRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "이미 처리 중인 발송은 취소할 수 없습니다. 현재 상태: " + entity.getStatus());
        }

        if (!entity.getRequestedAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("발송 시각이 지난 예약은 취소할 수 없습니다.");
        }

        entity.markCancelled(reason != null ? reason : "웹 콘솔 사용자 취소");

        log.info("[WebSend] 예약 취소 — sendId={}, memberId={}, reason={}", sendId, memberId, reason);
    }

    // ── 예약 목록 조회 ────────────────────────────────────────────────

    /**
     * 회원의 예약 발송 목록을 조회한다 (PENDING 상태 + 미래 requestedAt).
     *
     * @param memberId 로그인 회원 ID
     * @param pageable 페이지네이션
     * @return 예약 발송 요약 목록
     */
    @Transactional(readOnly = true)
    public Page<WebSendDto.ScheduledSummary> listScheduled(Long memberId, Pageable pageable) {
        return sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        memberId, SendRequestStatus.PENDING, LocalDateTime.now(), pageable)
                .map(WebSendDto.ScheduledSummary::from);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────

    /**
     * 회원의 PRODUCTION ACTIVE API Key 중 가장 최근 발급된 키를 반환한다.
     *
     * <p>웹 콘솔 발송은 상용 키를 자동 선택한다.
     * 상용 키가 없으면 EntityNotFoundException을 던진다.</p>
     */
    private ApiKey resolveProductionApiKey(Long memberId) {
        return apiKeyRepository
                .findFirstByMemberIdAndKeyTypeAndStatusOrderByCreatedAtDesc(
                        memberId, ApiKeyType.PRODUCTION,
                        com.wisecan.unified.domain.ApiKeyStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(
                        "활성 상용 API Key가 없습니다. 상용 키를 발급하세요. memberId=" + memberId));
    }

    private SendValidationContext buildContext(
            Long memberId, ApiKey apiKey,
            String callbackNumber, SendChannel channel,
            String messageBody, boolean isAdvertisement,
            int recipientCount, long unitCost
    ) {
        return new SendValidationContext(
                memberId,
                apiKey.getId(),
                apiKey.getKeyType(),
                callbackNumber,
                channel,
                messageBody,
                isAdvertisement,
                recipientCount,
                unitCost,
                NetworkType.PRODUCTION,
                null
        );
    }

    private SmsMessageType resolveSmsType(SendChannel channel, String messageBody) {
        return switch (channel) {
            case SMS, LMS, MMS -> SmsEncoding.resolveType(messageBody);
            case KAKAO, RCS -> null;
        };
    }
}
