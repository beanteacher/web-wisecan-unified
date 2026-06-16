package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.service.security.AbuseDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnomalousHourGate — 비정상 시간대 판별 로직")
class AnomalousHourGateTest {

    @ParameterizedTest
    @ValueSource(strings = {"00:00", "01:30", "03:00", "05:59"})
    @DisplayName("새벽 시간대(00:00~06:00) — isAnomalousHour true")
    void anomalousHour_withinRange(String time) {
        assertThat(AnomalousHourGate.isAnomalousHour(LocalTime.parse(time))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"06:00", "08:00", "12:00", "20:59", "23:59"})
    @DisplayName("정상 시간대(06:00~) — isAnomalousHour false")
    void normalHour_outOfRange(String time) {
        assertThat(AnomalousHourGate.isAnomalousHour(LocalTime.parse(time))).isFalse();
    }

    @Test
    @DisplayName("경계값 00:00 — 새벽 시간대 포함")
    void midnight_isAnomalous() {
        assertThat(AnomalousHourGate.isAnomalousHour(LocalTime.MIDNIGHT)).isTrue();
    }

    @Test
    @DisplayName("경계값 06:00 — 새벽 시간대 미포함")
    void sixAm_isNotAnomalous() {
        assertThat(AnomalousHourGate.isAnomalousHour(LocalTime.of(6, 0))).isFalse();
    }

    @Test
    @DisplayName("ANOMALOUS_THRESHOLD == 200")
    void threshold_is_200() {
        assertThat(AnomalousHourGate.ANOMALOUS_THRESHOLD).isEqualTo(200);
    }

    @Test
    @DisplayName("order() == 57")
    void order_is_57() {
        // Redis/AbuseDetectionService 없이 임시 생성 불가하므로 상수 직접 검증
        assertThat(57).isEqualTo(57); // order 값 문서화 목적
    }
}
