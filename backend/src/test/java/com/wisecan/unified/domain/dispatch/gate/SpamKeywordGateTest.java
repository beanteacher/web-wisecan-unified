package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SpamKeywordGate — 스팸 키워드 필터 검증")
class SpamKeywordGateTest {

    private final SpamKeywordGate gate = new SpamKeywordGate();

    private SendValidationContext ctx(String body) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, body, false, 1, 10L, NetworkType.TEST, null);
    }

    @Test
    @DisplayName("정상 메시지 — 통과")
    void normalMessage_passes() {
        assertThatCode(() -> gate.validate(ctx("안녕하세요. 이번 달 이벤트 안내드립니다."))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null 본문 — 통과 (검사 생략)")
    void nullBody_passes() {
        assertThatCode(() -> gate.validate(ctx(null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 본문 — 통과 (검사 생략)")
    void blankBody_passes() {
        assertThatCode(() -> gate.validate(ctx("   "))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"대출 안내드립니다", "무료대출 가능합니다", "도박 사이트", "성인 콘텐츠", "마약 구매"})
    @DisplayName("스팸 키워드 포함 — SPAM_KEYWORD_DETECTED")
    void spamKeyword_throws(String body) {
        assertThatThrownBy(() -> gate.validate(ctx(body)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SPAM_KEYWORD_DETECTED));
    }

    @Test
    @DisplayName("대문자 스팸 키워드(SPAM) — 대소문자 무관 감지")
    void uppercaseSpam_detected() {
        assertThatThrownBy(() -> gate.validate(ctx("SPAM 주의 메시지")))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SPAM_KEYWORD_DETECTED));
    }

    @Test
    @DisplayName("투자보장 키워드 — SPAM_KEYWORD_DETECTED")
    void investmentGuarantee_throws() {
        assertThatThrownBy(() -> gate.validate(ctx("원금보장! 지금 가입하세요.")))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SPAM_KEYWORD_DETECTED));
    }

    @Test
    @DisplayName("order() == 50")
    void order_is_50() {
        assertThat(gate.order()).isEqualTo(50);
    }
}
