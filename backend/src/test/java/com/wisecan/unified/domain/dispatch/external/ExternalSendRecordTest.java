package com.wisecan.unified.domain.dispatch.external;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExternalSendRecord 단위 테스트 — W-204 합의 스펙 검증.
 *
 * <p>채널별 msgType·msgSubType 산출 및 핵심 필드 매핑을 검증한다.</p>
 */
class ExternalSendRecordTest {

    // ── 픽스처 ────────────────────────────────────────────────────────

    private SendRequest buildRequest(SendChannel channel, boolean isAd, String templateCode) {
        return SendRequest.builder()
                .memberId(1L)
                .apiKeyId(10L)
                .channel(channel)
                .smsType(channel == SendChannel.SMS || channel == SendChannel.LMS
                        ? SmsMessageType.SMS : null)
                .callbackNumber("01012345678")
                .recipientNumbers("01099998888")
                .recipientCount(1)
                .subject(null)
                .messageBody("테스트 메시지입니다.")
                .isAdvertisement(isAd)
                .senderKey(channel == SendChannel.KAKAO ? "test-sender-key" : null)
                .templateCode(templateCode)
                .requestedAt(LocalDateTime.of(2026, 6, 16, 10, 0))
                .groupId(null)
                .routingMeta(null)
                .unitCost(20L)
                .build();
    }

    // ── SMS 일반 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SMS 일반 발송 — msgType=SMS, msgSubType=NORM")
    void smsNormal_msgTypeAndSubType() {
        SendRequest req = buildRequest(SendChannel.SMS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("SMS");
        assertThat(record.msgSubType()).isEqualTo("NORM");
        assertThat(record.messageState()).isEqualTo(0);
    }

    @Test
    @DisplayName("SMS 광고 발송 — msgSubType=AD")
    void smsAd_msgSubTypeAD() {
        SendRequest req = buildRequest(SendChannel.SMS, true, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("SMS");
        assertThat(record.msgSubType()).isEqualTo("AD");
    }

    // ── LMS/MMS ────────────────────────────────────────────────────

    @Test
    @DisplayName("LMS 일반 발송 — msgType=LMS, msgSubType=NORM")
    void lmsNormal_msgTypeAndSubType() {
        SendRequest req = buildRequest(SendChannel.LMS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("LMS");
        assertThat(record.msgSubType()).isEqualTo("NORM");
    }

    @Test
    @DisplayName("MMS 광고 발송 — msgType=MMS, msgSubType=AD")
    void mmsAd_msgTypeAndSubType() {
        SendRequest req = buildRequest(SendChannel.MMS, true, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("MMS");
        assertThat(record.msgSubType()).isEqualTo("AD");
    }

    // ── 카카오 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("카카오 알림톡 — msgType=AT, msgSubType=AT")
    void kakao_msgTypeAT() {
        SendRequest req = buildRequest(SendChannel.KAKAO, false, "TMPL_001");

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("AT");
        assertThat(record.msgSubType()).isEqualTo("AT");
        assertThat(record.senderKey()).isEqualTo("test-sender-key");
        assertThat(record.templateCode()).isEqualTo("TMPL_001");
    }

    // ── RCS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("RCS 템플릿 발송 — msgType=RCS, msgSubType=TMPL")
    void rcsTemplate_msgSubTypeTMPL() {
        SendRequest req = buildRequest(SendChannel.RCS, false, "RCS_TMPL_001");

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("RCS");
        assertThat(record.msgSubType()).isEqualTo("TMPL");
    }

    @Test
    @DisplayName("RCS 일반 텍스트 — msgSubType=TEXT")
    void rcsText_msgSubTypeTEXT() {
        SendRequest req = buildRequest(SendChannel.RCS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.msgType()).isEqualTo("RCS");
        assertThat(record.msgSubType()).isEqualTo("TEXT");
    }

    // ── 필드 매핑 ────────────────────────────────────────────────────

    @Test
    @DisplayName("etcChar1 에 sendId(ULID) 가 저장된다")
    void etcChar1_containsSendId() {
        SendRequest req = buildRequest(SendChannel.SMS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, "routing-meta-stub", null);

        assertThat(record.etcChar1()).isEqualTo(req.getSendId());
        assertThat(record.etcChar1()).hasSize(26); // ULID 길이
    }

    @Test
    @DisplayName("etcChar2 에 routingMeta 가 저장된다 (회원 비노출 필드)")
    void etcChar2_containsRoutingMeta() {
        SendRequest req = buildRequest(SendChannel.SMS, false, null);
        String routingMeta = "{\"provider\":\"LG_CNS\",\"routeId\":42}";

        ExternalSendRecord record = ExternalSendRecord.from(req, routingMeta, null);

        assertThat(record.etcChar2()).isEqualTo(routingMeta);
    }

    @Test
    @DisplayName("messageState 는 항상 0(init) 으로 초기화된다")
    void messageState_alwaysZero() {
        SendRequest req = buildRequest(SendChannel.SMS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.messageState()).isEqualTo(0);
    }

    @Test
    @DisplayName("userId 는 내부 memberId 와 동일하다")
    void userId_equalsMemberId() {
        SendRequest req = buildRequest(SendChannel.SMS, false, null);

        ExternalSendRecord record = ExternalSendRecord.from(req, null, null);

        assertThat(record.userId()).isEqualTo(req.getMemberId());
    }
}
