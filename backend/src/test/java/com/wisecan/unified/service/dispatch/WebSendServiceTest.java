package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.*;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import com.wisecan.unified.dto.dispatch.WebSendDto;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;

/**
 * WebSendService 단위 테스트 (W-206).
 *
 * <p>SendValidationService·SendRequestRepository·ApiKeyRepository를 Mock 처리해
 * 웹 콘솔 발송 서비스 로직(엔티티 구성·DTO 변환·예약 취소 흐름)만 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class WebSendServiceTest {

    @Mock
    private SendValidationService sendValidationService;

    @Mock
    private SendRequestRepository sendRequestRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private WebSendService webSendService;

    private static final Long MEMBER_ID  = 1L;
    private static final Long API_KEY_ID = 10L;

    @BeforeEach
    void setUp() {
        doNothing().when(sendValidationService).validate(any(SendValidationContext.class));
        given(apiKeyRepository.findFirstByMemberIdAndKeyTypeAndStatusOrderByCreatedAtDesc(
                MEMBER_ID, ApiKeyType.PRODUCTION, ApiKeyStatus.ACTIVE))
                .willReturn(Optional.of(productionApiKey()));
    }

    // ── send (단건) ───────────────────────────────────────────────────

    @Test
    @DisplayName("send() — SMS 채널 단건 적재: PENDING 응답 반환")
    void send_smsChannel_returnsPendingAcceptResponse() {
        WebSendDto.SingleRequest request = new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "안녕하세요", false, null, null
        );
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, LocalDateTime.now());
        given(sendRequestRepository.save(any(SendRequest.class))).willReturn(fakeEntity);

        WebSendDto.AcceptResponse response = webSendService.send(MEMBER_ID, request);

        assertThat(response.sendId()).hasSize(26);
        assertThat(response.status()).isEqualTo(SendRequestStatus.PENDING);
        assertThat(response.recipientCount()).isEqualTo(1);
        then(sendValidationService).should().validate(any(SendValidationContext.class));
        then(sendRequestRepository).should().save(any(SendRequest.class));
    }

    @Test
    @DisplayName("send() — 복수 수신자: recipientNumbers CSV로 저장")
    void send_multipleRecipients_savedAsCsv() {
        List<String> recipients = List.of("01011111111", "01022222222", "01033333333");
        WebSendDto.SingleRequest request = new WebSendDto.SingleRequest(
                "01012345678", recipients, SendChannel.SMS,
                null, "테스트", false, null, null
        );
        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 3, LocalDateTime.now());
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        webSendService.send(MEMBER_ID, request);

        SendRequest captured = captor.getValue();
        assertThat(captured.getRecipientCount()).isEqualTo(3);
        assertThat(captured.getRecipientNumbers()).contains("01011111111");
        assertThat(captured.getRecipientNumbers()).contains("01022222222");
    }

    @Test
    @DisplayName("send() — KAKAO 채널: smsType null, senderKey·templateCode 저장")
    void send_kakaoChannel_smsTypeNullAndKakaoFieldsSaved() {
        WebSendDto.SingleRequest request = new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.KAKAO,
                "주문 완료", "주문이 완료되었습니다.", false,
                "SENDER_KEY_XYZ", "ORDER_COMPLETE_001"
        );
        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.KAKAO, null, 1, LocalDateTime.now());
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        webSendService.send(MEMBER_ID, request);

        SendRequest captured = captor.getValue();
        assertThat(captured.getSmsType()).isNull();
        assertThat(captured.getSenderKey()).isEqualTo("SENDER_KEY_XYZ");
        assertThat(captured.getTemplateCode()).isEqualTo("ORDER_COMPLETE_001");
    }

    @Test
    @DisplayName("send() — 상용 API Key 없으면 EntityNotFoundException")
    void send_noProductionApiKey_throwsEntityNotFoundException() {
        given(apiKeyRepository.findFirstByMemberIdAndKeyTypeAndStatusOrderByCreatedAtDesc(
                MEMBER_ID, ApiKeyType.PRODUCTION, ApiKeyStatus.ACTIVE))
                .willReturn(Optional.empty());

        WebSendDto.SingleRequest request = new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "테스트", false, null, null
        );

        assertThatThrownBy(() -> webSendService.send(MEMBER_ID, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("활성 상용 API Key가 없습니다");
    }

    @Test
    @DisplayName("send() — 검증 컨텍스트에 NetworkType.PRODUCTION 전달")
    void send_validationContextHasProductionNetworkType() {
        WebSendDto.SingleRequest request = new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "테스트", false, null, null
        );
        ArgumentCaptor<SendValidationContext> ctxCaptor = ArgumentCaptor.forClass(SendValidationContext.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, LocalDateTime.now());
        given(sendRequestRepository.save(any())).willReturn(fakeEntity);

        webSendService.send(MEMBER_ID, request);

        then(sendValidationService).should().validate(ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().networkType()).isEqualTo(NetworkType.PRODUCTION);
    }

    // ── sendBulk ──────────────────────────────────────────────────────

    @Test
    @DisplayName("sendBulk() — 1000명 수신자: recipientCount=1000, groupId 부여")
    void sendBulk_thousandRecipients_groupIdAssigned() {
        List<String> recipients = java.util.stream.IntStream
                .rangeClosed(1, 1000)
                .mapToObj(i -> String.format("0101%07d", i))
                .toList();
        WebSendDto.BulkRequest request = new WebSendDto.BulkRequest(
                "01012345678", SendChannel.SMS, null, "일괄 발송", false, null, null, recipients
        );
        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1000, LocalDateTime.now());
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        WebSendDto.AcceptResponse response = webSendService.sendBulk(MEMBER_ID, request);

        SendRequest captured = captor.getValue();
        assertThat(captured.getRecipientCount()).isEqualTo(1000);
        assertThat(captured.getGroupId()).isNotNull();
        assertThat(response.recipientCount()).isEqualTo(1000);
    }

    @Test
    @DisplayName("sendBulk() — 검증 컨텍스트에 실제 recipientCount 전달")
    void sendBulk_validationContextHasCorrectRecipientCount() {
        List<String> recipients = List.of("01011111111", "01022222222");
        WebSendDto.BulkRequest request = new WebSendDto.BulkRequest(
                "01012345678", SendChannel.LMS, "제목", "본문", false, null, null, recipients
        );
        ArgumentCaptor<SendValidationContext> ctxCaptor = ArgumentCaptor.forClass(SendValidationContext.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.LMS, SmsMessageType.LMS, 2, LocalDateTime.now());
        given(sendRequestRepository.save(any())).willReturn(fakeEntity);

        webSendService.sendBulk(MEMBER_ID, request);

        then(sendValidationService).should().validate(ctxCaptor.capture());
        assertThat(ctxCaptor.getValue().recipientCount()).isEqualTo(2);
    }

    // ── sendScheduled ─────────────────────────────────────────────────

    @Test
    @DisplayName("sendScheduled() — requestedAt이 scheduledAt으로 저장")
    void sendScheduled_requestedAtEqualsScheduledAt() {
        LocalDateTime future = LocalDateTime.now().plusHours(3);
        WebSendDto.ScheduledRequest request = new WebSendDto.ScheduledRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "예약 메시지", false, null, null, future
        );
        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, future);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        WebSendDto.AcceptResponse response = webSendService.sendScheduled(MEMBER_ID, request);

        assertThat(captor.getValue().getRequestedAt()).isEqualTo(future);
        assertThat(response.scheduledAt()).isEqualTo(future);
    }

    @Test
    @DisplayName("sendScheduled() — RCS 채널: smsType null")
    void sendScheduled_rcsChannel_smsTypeNull() {
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        WebSendDto.ScheduledRequest request = new WebSendDto.ScheduledRequest(
                "01012345678", List.of("01099999999"), SendChannel.RCS,
                "RCS 메시지", "RCS 내용", false, null, null, future
        );
        ArgumentCaptor<SendRequest> captor = ArgumentCaptor.forClass(SendRequest.class);
        SendRequest fakeEntity = buildFakeSavedEntity(SendChannel.RCS, null, 1, future);
        given(sendRequestRepository.save(captor.capture())).willReturn(fakeEntity);

        webSendService.sendScheduled(MEMBER_ID, request);

        assertThat(captor.getValue().getSmsType()).isNull();
    }

    // ── cancelScheduled ───────────────────────────────────────────────

    @Test
    @DisplayName("cancelScheduled() — PENDING + 미래 시각: markCancelled 호출")
    void cancelScheduled_pendingFuture_markedCancelled() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        SendRequest entity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, future);
        given(sendRequestRepository.findBySendId(entity.getSendId()))
                .willReturn(Optional.of(entity));

        webSendService.cancelScheduled(MEMBER_ID, entity.getSendId(), "테스트 취소");

        assertThat(entity.getStatus()).isEqualTo(SendRequestStatus.CANCELLED);
        assertThat(entity.getFailReason()).isEqualTo("테스트 취소");
    }

    @Test
    @DisplayName("cancelScheduled() — 존재하지 않는 sendId: EntityNotFoundException")
    void cancelScheduled_unknownSendId_throwsEntityNotFoundException() {
        given(sendRequestRepository.findBySendId("NOTEXIST00000000000000000"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> webSendService.cancelScheduled(MEMBER_ID, "NOTEXIST00000000000000000", null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("cancelScheduled() — 다른 회원의 발송: EntityNotFoundException")
    void cancelScheduled_differentMember_throwsEntityNotFoundException() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        // memberId=999의 엔티티를 memberId=1이 취소 시도
        SendRequest entity = SendRequest.builder()
                .memberId(999L)
                .apiKeyId(API_KEY_ID)
                .channel(SendChannel.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(1)
                .messageBody("테스트")
                .isAdvertisement(false)
                .requestedAt(future)
                .unitCost(20L)
                .build();
        given(sendRequestRepository.findBySendId(entity.getSendId()))
                .willReturn(Optional.of(entity));

        assertThatThrownBy(() -> webSendService.cancelScheduled(MEMBER_ID, entity.getSendId(), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("cancelScheduled() — QUEUED 상태: IllegalStateException")
    void cancelScheduled_queuedStatus_throwsIllegalStateException() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        SendRequest entity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, future);
        entity.markQueued(12345L); // QUEUED 상태로 전환
        given(sendRequestRepository.findBySendId(entity.getSendId()))
                .willReturn(Optional.of(entity));

        assertThatThrownBy(() -> webSendService.cancelScheduled(MEMBER_ID, entity.getSendId(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리 중인 발송");
    }

    @Test
    @DisplayName("cancelScheduled() — 과거 requestedAt: IllegalStateException")
    void cancelScheduled_pastScheduledAt_throwsIllegalStateException() {
        LocalDateTime past = LocalDateTime.now().minusHours(1);
        SendRequest entity = buildFakeSavedEntity(SendChannel.SMS, SmsMessageType.SMS, 1, past);
        given(sendRequestRepository.findBySendId(entity.getSendId()))
                .willReturn(Optional.of(entity));

        assertThatThrownBy(() -> webSendService.cancelScheduled(MEMBER_ID, entity.getSendId(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("발송 시각이 지난");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    private ApiKey productionApiKey() {
        return ApiKey.builder()
                .keyName("상용키").keyPrefix("wc_live").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.PRODUCTION)
                .scopes(Set.of(com.wisecan.unified.domain.ApiKeyScope.SEND_SMS))
                .build();
    }

    private SendRequest buildFakeSavedEntity(
            SendChannel channel, SmsMessageType smsType, int recipientCount, LocalDateTime requestedAt) {
        return SendRequest.builder()
                .memberId(MEMBER_ID)
                .apiKeyId(API_KEY_ID)
                .channel(channel)
                .smsType(smsType)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(recipientCount)
                .messageBody("테스트")
                .isAdvertisement(false)
                .requestedAt(requestedAt)
                .unitCost(20L)
                .build();
    }
}
