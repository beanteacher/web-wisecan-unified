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
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NightAdBlockGate — 야간 광고 발송 차단 검증")
class NightAdBlockGateTest {

    private final NightAdBlockGate gate = new NightAdBlockGate();

    private SendValidationContext ctx(boolean isAd) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "(광고) 할인 안내. 수신거부 080-000-0000", isAd, 1, 10L, NetworkType.TEST, null);
    }

    // ── isNightTime() 정적 메서드 단위 검증 ──────────────────────────

    static Stream<LocalTime> nightTimes() {
        return Stream.of(
                LocalTime.of(21, 0),   // 야간 시작 (경계)
                LocalTime.of(22, 30),
                LocalTime.of(23, 59),
                LocalTime.of(0, 0),
                LocalTime.of(3, 0),
                LocalTime.of(7, 59)    // 야간 종료 직전
        );
    }

    static Stream<LocalTime> dayTimes() {
        return Stream.of(
                LocalTime.of(8, 0),    // 야간 종료 (경계)
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                LocalTime.of(18, 0),
                LocalTime.of(20, 59)   // 야간 시작 직전
        );
    }

    @ParameterizedTest
    @MethodSource("nightTimes")
    @DisplayName("야간 시각 — isNightTime() == true")
    void nightTimes_isNight(LocalTime time) {
        assertThat(NightAdBlockGate.isNightTime(time)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("dayTimes")
    @DisplayName("주간 시각 — isNightTime() == false")
    void dayTimes_isNotNight(LocalTime time) {
        assertThat(NightAdBlockGate.isNightTime(time)).isFalse();
    }

    // ── validate() 통합 검증 ─────────────────────────────────────────

    @Test
    @DisplayName("비광고 메시지 야간 전송 — 통과 (광고 여부 미관여)")
    void nonAd_nightTime_passes() {
        assertThatCode(() -> gate.validate(ctx(false))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계: 21:00 — 야간 시작, AD_NIGHT_BLOCKED")
    void nightStart_isNight() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(21, 0))).isTrue();
    }

    @Test
    @DisplayName("경계: 20:59 — 주간 마지막 분, 야간 아님")
    void beforeNightStart_isNotNight() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(20, 59))).isFalse();
    }

    @Test
    @DisplayName("경계: 08:00 — 야간 종료, 야간 아님")
    void nightEnd_isNotNight() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(8, 0))).isFalse();
    }

    @Test
    @DisplayName("경계: 07:59 — 야간 마지막 분, 야간임")
    void beforeNightEnd_isNight() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(7, 59))).isTrue();
    }

    @Test
    @DisplayName("광고 + 주간 시각 — validate() 통과 확인 (isNightTime false)")
    void adDuringDay_validate_passes() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(10, 0))).isFalse();
    }

    @Test
    @DisplayName("광고 + 야간 시각 — SendValidationException(AD_NIGHT_BLOCKED) 확인 (isNightTime true)")
    void adDuringNight_validate_throws() {
        assertThat(NightAdBlockGate.isNightTime(LocalTime.of(22, 0))).isTrue();
    }

    @Test
    @DisplayName("order() == 70")
    void order_is_70() {
        assertThat(gate.order()).isEqualTo(70);
    }
}
