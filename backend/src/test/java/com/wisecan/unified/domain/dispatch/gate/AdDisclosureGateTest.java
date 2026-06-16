package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdDisclosureGate — 광고 의무 표기 검증")
class AdDisclosureGateTest {

    private final AdDisclosureGate gate = new AdDisclosureGate();

    private SendValidationContext ctx(boolean isAd, String body) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, body, isAd, 1, 10L, NetworkType.TEST, null);
    }

    @Test
    @DisplayName("비광고 메시지 — 검사 없이 통과")
    void nonAd_passes() {
        assertThatCode(() -> gate.validate(ctx(false, "일반 안내 문자입니다."))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("광고 + (광고) 표기 + 080 번호 — 통과")
    void validAd_passes() {
        String body = "(광고) 특별 할인 이벤트! 수신거부 080-000-0000";
        assertThatCode(() -> gate.validate(ctx(true, body))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("광고 + (광고) 표기 없음 — AD_DISCLOSURE_MISSING")
    void adWithoutPrefix_throws() {
        String body = "특별 할인 이벤트! 수신거부 080-000-0000";
        assertThatThrownBy(() -> gate.validate(ctx(true, body)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.AD_DISCLOSURE_MISSING));
    }

    @Test
    @DisplayName("광고 + (광고) 표기 있음 + 080 없음 — AD_OPT_OUT_MISSING")
    void adWithoutOptOut_throws() {
        String body = "(광고) 특별 할인 이벤트! 지금 바로 신청하세요.";
        assertThatThrownBy(() -> gate.validate(ctx(true, body)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.AD_OPT_OUT_MISSING));
    }

    @Test
    @DisplayName("광고 + null 본문 — AD_DISCLOSURE_MISSING")
    void adWithNullBody_throws() {
        assertThatThrownBy(() -> gate.validate(ctx(true, null)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.AD_DISCLOSURE_MISSING));
    }

    @Test
    @DisplayName("비광고 + (광고) 표기 없음 — 비광고이므로 통과 (검사 없음)")
    void nonAdWithoutPrefix_passes() {
        assertThatCode(() -> gate.validate(ctx(false, "080 번호 없고 (광고) 없는 문자")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("order() == 60")
    void order_is_60() {
        assertThat(gate.order()).isEqualTo(60);
    }
}
