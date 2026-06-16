package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.adapter.dispatch.ExternalDispatchAdapter;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import com.wisecan.unified.domain.dispatch.external.ExternalDispatchPollingResult;
import com.wisecan.unified.domain.dispatch.external.ExternalSendRecord;
import com.wisecan.unified.domain.dispatch.external.ExternalSendResult;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * ExternalDispatchService 단위 테스트 — W-204.
 *
 * <p>ExternalDispatchAdapter·SendRequestRepository 를 Mock 처리해
 * 서비스 위임 흐름·상태 전이를 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class ExternalDispatchServiceTest {

    @Mock
    private ExternalDispatchAdapter externalDispatchAdapter;

    @Mock
    private SendRequestRepository sendRequestRepository;

    @InjectMocks
    private ExternalDispatchService externalDispatchService;

    private static final String SEND_ID       = "01ABCDEFGHIJKLMNOPQRSTUVWX";
    private static final Long   EXTERNAL_MSG_ID = 1_000_001L;

    // ── dispatch — 성공 경로 ─────────────────────────────────────────

    @Test
    @DisplayName("dispatch — 외부 INSERT 성공 시 SendRequest 상태가 QUEUED 로 변경된다")
    void dispatch_success_markQueued() {
        SendRequest req = buildSmsRequest();
        given(sendRequestRepository.findBySendId(SEND_ID)).willReturn(Optional.of(req));
        given(externalDispatchAdapter.insert(any(ExternalSendRecord.class)))
                .willReturn(ExternalSendResult.success(EXTERNAL_MSG_ID));

        ExternalSendResult result = externalDispatchService.dispatch(SEND_ID, null);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo(EXTERNAL_MSG_ID);
        assertThat(req.getStatus()).isEqualTo(SendRequestStatus.QUEUED);
        assertThat(req.getExternalMsgId()).isEqualTo(EXTERNAL_MSG_ID);
    }

    @Test
    @DisplayName("dispatch — 외부 INSERT 성공 시 어댑터에 ExternalSendRecord 가 전달된다")
    void dispatch_success_adapterCalledWithRecord() {
        SendRequest req = buildSmsRequest();
        given(sendRequestRepository.findBySendId(SEND_ID)).willReturn(Optional.of(req));
        given(externalDispatchAdapter.insert(any(ExternalSendRecord.class)))
                .willReturn(ExternalSendResult.success(EXTERNAL_MSG_ID));

        externalDispatchService.dispatch(SEND_ID, "routing-meta");

        ArgumentCaptor<ExternalSendRecord> captor = ArgumentCaptor.forClass(ExternalSendRecord.class);
        then(externalDispatchAdapter).should().insert(captor.capture());

        ExternalSendRecord captured = captor.getValue();
        assertThat(captured.msgType()).isEqualTo("SMS");
        assertThat(captured.messageState()).isEqualTo(0);
        assertThat(captured.etcChar1()).isEqualTo(SEND_ID);
        assertThat(captured.etcChar2()).isEqualTo("routing-meta");
    }

    // ── dispatch — 실패 경로 ─────────────────────────────────────────

    @Test
    @DisplayName("dispatch — 외부 INSERT 실패 시 SendRequest 상태가 FAILED 로 변경된다")
    void dispatch_failure_markFailed() {
        SendRequest req = buildSmsRequest();
        given(sendRequestRepository.findBySendId(SEND_ID)).willReturn(Optional.of(req));
        given(externalDispatchAdapter.insert(any(ExternalSendRecord.class)))
                .willReturn(ExternalSendResult.failure("외부 시스템 연결 오류"));

        ExternalSendResult result = externalDispatchService.dispatch(SEND_ID, null);

        assertThat(result.success()).isFalse();
        assertThat(result.msgId()).isNull();
        assertThat(req.getStatus()).isEqualTo(SendRequestStatus.FAILED);
        assertThat(req.getFailReason()).isEqualTo("외부 시스템 연결 오류");
    }

    @Test
    @DisplayName("dispatch — sendId 가 존재하지 않으면 EntityNotFoundException")
    void dispatch_notFound_throwsException() {
        given(sendRequestRepository.findBySendId("NOTEXIST12345678901234567"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                externalDispatchService.dispatch("NOTEXIST12345678901234567", null))
                .isInstanceOf(EntityNotFoundException.class);

        then(externalDispatchAdapter).shouldHaveNoInteractions();
    }

    // ── poll ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("poll — 어댑터 poll 결과를 그대로 반환한다")
    void poll_delegatesToAdapter() {
        ExternalDispatchPollingResult expected = new ExternalDispatchPollingResult(
                EXTERNAL_MSG_ID, 3, "0000", LocalDateTime.now(), "KT");
        given(externalDispatchAdapter.poll(EXTERNAL_MSG_ID)).willReturn(expected);

        ExternalDispatchPollingResult result = externalDispatchService.poll(EXTERNAL_MSG_ID);

        assertThat(result).isEqualTo(expected);
        assertThat(result.isTerminal()).isTrue();
        assertThat(result.isDelivered()).isTrue();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private SendRequest buildSmsRequest() {
        return SendRequest.builder()
                .memberId(1L)
                .apiKeyId(10L)
                .channel(SendChannel.SMS)
                .smsType(SmsMessageType.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01099998888")
                .recipientCount(1)
                .subject(null)
                .messageBody("테스트 메시지입니다.")
                .isAdvertisement(false)
                .senderKey(null)
                .templateCode(null)
                .requestedAt(LocalDateTime.now())
                .groupId(null)
                .routingMeta(null)
                .unitCost(20L)
                .build();
    }
}
