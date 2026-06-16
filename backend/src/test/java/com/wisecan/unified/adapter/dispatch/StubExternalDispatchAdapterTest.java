package com.wisecan.unified.adapter.dispatch;

import com.wisecan.unified.domain.dispatch.external.ExternalDispatchPollingResult;
import com.wisecan.unified.domain.dispatch.external.ExternalSendRecord;
import com.wisecan.unified.domain.dispatch.external.ExternalSendResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StubExternalDispatchAdapter 단위 테스트 — W-204.
 *
 * <p>스텁 동작(항상 성공, msg_id 순번 증가, polling 즉시 finished)을 검증한다.</p>
 */
class StubExternalDispatchAdapterTest {

    private StubExternalDispatchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StubExternalDispatchAdapter();
    }

    // ── insert ────────────────────────────────────────────────────────

    @Test
    @DisplayName("insert — 항상 성공, msgId non-null")
    void insert_alwaysSuccess() {
        ExternalSendRecord record = buildRecord("SMS", "NORM");

        ExternalSendResult result = adapter.insert(record);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isNotNull();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("insert — 연속 호출 시 msgId 순번이 증가한다")
    void insert_msgIdIncrementsSequentially() {
        ExternalSendRecord record = buildRecord("SMS", "NORM");

        ExternalSendResult first  = adapter.insert(record);
        ExternalSendResult second = adapter.insert(record);

        assertThat(second.msgId()).isGreaterThan(first.msgId());
    }

    @Test
    @DisplayName("insert — 카카오 채널(AT)도 성공한다")
    void insert_kakaoChannel_success() {
        ExternalSendRecord record = buildRecord("AT", "AT");

        ExternalSendResult result = adapter.insert(record);

        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isNotNull();
    }

    // ── poll ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("poll — messageState=3(finished), resultCode=0000 즉시 반환")
    void poll_returnsFinishedState() {
        ExternalDispatchPollingResult pollResult = adapter.poll(1_000_000L);

        assertThat(pollResult.msgId()).isEqualTo(1_000_000L);
        assertThat(pollResult.messageState()).isEqualTo(3);
        assertThat(pollResult.resultCode()).isEqualTo("0000");
        assertThat(pollResult.resultDeliverDate()).isNotNull();
    }

    @Test
    @DisplayName("poll — isTerminal() 이 true")
    void poll_isTerminal() {
        ExternalDispatchPollingResult pollResult = adapter.poll(999L);

        assertThat(pollResult.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("poll — isDelivered() 가 true (messageState=3, resultCode=0000)")
    void poll_isDelivered() {
        ExternalDispatchPollingResult pollResult = adapter.poll(999L);

        assertThat(pollResult.isDelivered()).isTrue();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private ExternalSendRecord buildRecord(String msgType, String msgSubType) {
        return new ExternalSendRecord(
                msgType,
                msgSubType,
                "01012345678",
                "01099998888",
                1,
                null,
                "테스트 메시지",
                null,
                null,
                null,
                0,
                LocalDateTime.now(),
                null,
                20L,
                1L,
                0,
                "01ABCDEFGHIJKLMNOPQRSTUVWX",
                null
        );
    }
}
