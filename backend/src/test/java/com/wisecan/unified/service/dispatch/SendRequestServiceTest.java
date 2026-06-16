package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.*;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import com.wisecan.unified.dto.dispatch.SendRequestDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

/**
 * SendRequestService 단위 테스트.
 *
 * <p>SendValidationService·SendRequestRepository·ApiKeyRepository를 Mock 처리해
 * 서비스 로직(엔티티 구성·DTO 변환·위임 흐름)만 검증한다.</p>
 *
 * <p>W-205: ApiKeyRepository mock 추가 — keyType 기반 자동 망 결정 흐름 검증.</p>
 */
@ExtendWith(MockitoExtension.class)
class SendRequestServiceTest {

    @Mock
    private SendValidationService sendValidationService;

    @Mock
    private SendRequestRepository sendRequestRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private SendRequestService sendRequestService;

    private static final Long MEMBER_ID  = 1L;
    private static final Long API_KEY_ID = 10L;
    private static final long UNIT_COST  = 20L;

    @BeforeEach
    void setUp() {
        // 검증 게이트 — void 메서드이므로 별도 stub 없음 (Mockito 기본 동작 = no-op)
        doNothing().when(sendValidationService).validate(any(SendValidationContext.class));
        // W-205: TEST 키 기본 stub — ApiKey 로드가 필요한 모든 테스트에서 사용
        given(apiKeyRepository.findById(API_KEY_ID)).willReturn(Optional.of(testApiKey()));
    }

    // ── sendSingle ────────────────────────────────────────────────────

    @Test
    @DisplayName("sendSingle() — SMS 채널: 검증·적재 호출 후 PENDING 응답 반환")
    void sendSingle_smsChannel_returnsPendingResponse() {
        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.SMS,
                null, "안녕하세요", false, null, null, null
        );

        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.save(any(SendRequest.class))).willReturn(fakeEntity);

        SendRequestDto.AcceptResponse response = sendRequestService.sendSingle(
                MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        assertThat(response.sendId()).hasSize(26);
        assertThat(response.status()).isEqualTo(SendRequestStatus.PENDING);
        assertThat(response.recipientCount()).isEqualTo(1);
        assertThat(response.totalCost()).isEqualTo(20L);

        then(sendValidationService).should().validate(any(SendValidationContext.class));
        then(sendRequestRepository).should().save(any(SendRequest.class));
    }

    @Test
    @DisplayName("sendSingle() — KAKAO 채널: smsType은 null로 저장")
    void sendSingle_kakaoChannel_smsTypeIsNull() {
        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.KAKAO,
                "주문 완료", "주문이 완료되었습니다.", false,
                "SENDER_KEY_XYZ", "ORDER_COMPLETE_001", null
        );

        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.KAKAO, null, 1);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        sendRequestService.sendSingle(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        SendRequest captured = captor.getValue();
        assertThat(captured.getSmsType()).isNull();
        assertThat(captured.getSenderKey()).isEqualTo("SENDER_KEY_XYZ");
        assertThat(captured.getTemplateCode()).isEqualTo("ORDER_COMPLETE_001");
    }

    @Test
    @DisplayName("sendSingle() — 예약 발송: requestedAt이 scheduledAt으로 설정")
    void sendSingle_scheduledAt_setsRequestedAt() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.SMS,
                null, "예약 메시지", false, null, null, future
        );

        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        sendRequestService.sendSingle(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        assertThat(captor.getValue().getRequestedAt()).isEqualTo(future);
    }

    @Test
    @DisplayName("sendSingle() — 광고 메시지: isAdvertisement=Y 저장")
    void sendSingle_advertisement_flagSaved() {
        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.SMS,
                null, "(광고) 할인 이벤트", true, null, null, null
        );

        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        sendRequestService.sendSingle(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        assertThat(captor.getValue().isAdvertisementMessage()).isTrue();
    }

    @Test
    @DisplayName("W-205: sendSingle() — TEST 키, networkType null → 검증 컨텍스트에 NetworkType.TEST 자동 결정")
    void sendSingle_testKey_autoResolvesTestNetwork() {
        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.SMS,
                null, "테스트 메시지", false, null, null, null
        );

        ArgumentCaptor<SendValidationContext> ctxCaptor =
                ArgumentCaptor.forClass(SendValidationContext.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.save(any())).willReturn(fakeEntity);

        sendRequestService.sendSingle(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        then(sendValidationService).should().validate(ctxCaptor.capture());
        SendValidationContext capturedCtx = ctxCaptor.getValue();
        assertThat(capturedCtx.apiKeyType()).isEqualTo(ApiKeyType.TEST);
        assertThat(capturedCtx.networkType()).isEqualTo(NetworkType.TEST);
    }

    @Test
    @DisplayName("W-205: sendSingle() — PRODUCTION 키, networkType null → 검증 컨텍스트에 NetworkType.PRODUCTION 자동 결정")
    void sendSingle_productionKey_autoResolvesProductionNetwork() {
        given(apiKeyRepository.findById(API_KEY_ID)).willReturn(Optional.of(productionApiKey()));

        SendRequestDto.SingleRequest request = new SendRequestDto.SingleRequest(
                "01012345678", "01099999999", SendChannel.SMS,
                null, "상용 메시지", false, null, null, null
        );

        ArgumentCaptor<SendValidationContext> ctxCaptor =
                ArgumentCaptor.forClass(SendValidationContext.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.save(any())).willReturn(fakeEntity);

        sendRequestService.sendSingle(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        then(sendValidationService).should().validate(ctxCaptor.capture());
        SendValidationContext capturedCtx = ctxCaptor.getValue();
        assertThat(capturedCtx.apiKeyType()).isEqualTo(ApiKeyType.PRODUCTION);
        assertThat(capturedCtx.networkType()).isEqualTo(NetworkType.PRODUCTION);
    }

    // ── sendBulk ──────────────────────────────────────────────────────

    @Test
    @DisplayName("sendBulk() — 3명 수신자: recipientCount=3, groupId 부여")
    void sendBulk_threeRecipients_groupIdAssigned() {
        List<String> recipients = List.of("01011111111", "01022222222", "01033333333");
        SendRequestDto.BulkRequest request = new SendRequestDto.BulkRequest(
                "01012345678", recipients, SendChannel.SMS,
                null, "다건 테스트", false, null, null, null
        );

        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 3);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        SendRequestDto.AcceptResponse response = sendRequestService.sendBulk(
                MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        SendRequest captured = captor.getValue();
        assertThat(captured.getRecipientCount()).isEqualTo(3);
        assertThat(captured.getGroupId()).isNotNull();
        assertThat(captured.getRecipientNumbers()).contains("01011111111");
        assertThat(response.recipientCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("sendBulk() — 검증 컨텍스트에 recipientCount=3 전달")
    void sendBulk_validationContextHasCorrectRecipientCount() {
        List<String> recipients = List.of("01011111111", "01022222222", "01033333333");
        SendRequestDto.BulkRequest request = new SendRequestDto.BulkRequest(
                "01012345678", recipients, SendChannel.SMS,
                null, "다건 테스트", false, null, null, null
        );

        ArgumentCaptor<SendValidationContext> ctxCaptor =
                ArgumentCaptor.forClass(SendValidationContext.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 3);
        given(sendRequestRepository.save(any())).willReturn(fakeEntity);

        sendRequestService.sendBulk(MEMBER_ID, API_KEY_ID, UNIT_COST, request);

        then(sendValidationService).should().validate(ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().recipientCount()).isEqualTo(3);
        assertThat(ctxCaptor.getValue().unitCost()).isEqualTo(20L);
    }

    // ── getDetail ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getDetail() — 존재하는 sendId로 상세 조회 성공")
    void getDetail_existingSendId_returnsDetail() {
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1);
        given(sendRequestRepository.findBySendId(fakeEntity.getSendId()))
                .willReturn(Optional.of(fakeEntity));

        SendRequestDto.DetailResponse detail = sendRequestService.getDetail(fakeEntity.getSendId());

        assertThat(detail.sendId()).isEqualTo(fakeEntity.getSendId());
        assertThat(detail.channel()).isEqualTo(SendChannel.SMS);
        assertThat(detail.status()).isEqualTo(SendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("getDetail() — 존재하지 않는 sendId는 EntityNotFoundException")
    void getDetail_unknownSendId_throwsEntityNotFoundException() {
        given(sendRequestRepository.findBySendId("UNKNOWN00000000000000000000"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> sendRequestService.getDetail("UNKNOWN00000000000000000000"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("sendId=UNKNOWN00000000000000000000");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    /** TEST 유형 API Key 픽스처 */
    private ApiKey testApiKey() {
        return ApiKey.builder()
                .keyName("테스트키").keyPrefix("wc_test").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.TEST)
                .scopes(Set.of(ApiKeyScope.SEND_SMS))
                .build();
    }

    /** PRODUCTION 유형 API Key 픽스처 */
    private ApiKey productionApiKey() {
        return ApiKey.builder()
                .keyName("상용키").keyPrefix("wc_live").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.PRODUCTION)
                .scopes(Set.of(ApiKeyScope.SEND_SMS))
                .build();
    }

    /** 저장된 것처럼 보이는 SendRequest 픽스처 생성 */
    private SendRequest buildFakeSavedEntity(SendChannel channel, SmsMessageType smsType, int recipientCount) {
        SendRequest entity = SendRequest.builder()
                .memberId(MEMBER_ID)
                .apiKeyId(API_KEY_ID)
                .channel(channel)
                .smsType(smsType)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(recipientCount)
                .messageBody("테스트")
                .isAdvertisement(false)
                .requestedAt(LocalDateTime.now())
                .unitCost(UNIT_COST)
                .build();
        return entity;
    }
}
