package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AutoChargeConfigTest {

    private AutoChargeConfig config(String enabledYn, LocalDate expiresAt) {
        return AutoChargeConfig.builder()
                .memberId(1L)
                .paymentMethodId(1L)
                .thresholdAmount(5_000L)
                .chargeAmount(10_000L)
                .enabledYn(enabledYn)
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    @DisplayName("잔액이 임계치 이하이고 활성 상태이면 shouldTrigger = true")
    void shouldTrigger_belowThreshold_enabled_returnsTrue() {
        AutoChargeConfig cfg = config("Y", null);
        assertThat(cfg.shouldTrigger(5_000L)).isTrue();  // 임계치 동일
        assertThat(cfg.shouldTrigger(3_000L)).isTrue();  // 임계치 미만
    }

    @Test
    @DisplayName("잔액이 임계치 초과이면 shouldTrigger = false")
    void shouldTrigger_aboveThreshold_returnsFalse() {
        AutoChargeConfig cfg = config("Y", null);
        assertThat(cfg.shouldTrigger(5_001L)).isFalse();
    }

    @Test
    @DisplayName("비활성 상태이면 shouldTrigger = false")
    void shouldTrigger_disabled_returnsFalse() {
        AutoChargeConfig cfg = config("N", null);
        assertThat(cfg.shouldTrigger(1_000L)).isFalse();
    }

    @Test
    @DisplayName("만료된 설정이면 shouldTrigger = false")
    void shouldTrigger_expired_returnsFalse() {
        AutoChargeConfig cfg = config("Y", LocalDate.now().minusDays(1));
        assertThat(cfg.shouldTrigger(1_000L)).isFalse();
    }

    @Test
    @DisplayName("만료일이 오늘이면 isExpired = false (당일 포함 유효)")
    void isExpired_today_returnsFalse() {
        AutoChargeConfig cfg = config("Y", LocalDate.now());
        assertThat(cfg.isExpired()).isFalse();
    }

    @Test
    @DisplayName("만료일이 내일이면 isExpired = false")
    void isExpired_tomorrow_returnsFalse() {
        AutoChargeConfig cfg = config("Y", LocalDate.now().plusDays(1));
        assertThat(cfg.isExpired()).isFalse();
    }

    @Test
    @DisplayName("expiresAt = null 이면 isExpired = false (무기한)")
    void isExpired_noExpiry_returnsFalse() {
        AutoChargeConfig cfg = config("Y", null);
        assertThat(cfg.isExpired()).isFalse();
    }

    @Test
    @DisplayName("disableIfExpired — 만료된 경우 비활성화")
    void disableIfExpired_expired_disables() {
        AutoChargeConfig cfg = config("Y", LocalDate.now().minusDays(1));
        cfg.disableIfExpired();
        assertThat(cfg.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("disableIfExpired — 만료되지 않은 경우 활성 유지")
    void disableIfExpired_notExpired_remainsEnabled() {
        AutoChargeConfig cfg = config("Y", LocalDate.now().plusDays(10));
        cfg.disableIfExpired();
        assertThat(cfg.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("update — 필드 갱신 후 활성화")
    void update_updatesFieldsAndEnables() {
        AutoChargeConfig cfg = config("N", null);
        cfg.update(2L, 8_000L, 20_000L, LocalDate.now().plusDays(30));

        assertThat(cfg.getPaymentMethodId()).isEqualTo(2L);
        assertThat(cfg.getThresholdAmount()).isEqualTo(8_000L);
        assertThat(cfg.getChargeAmount()).isEqualTo(20_000L);
        assertThat(cfg.getExpiresAt()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(cfg.isEnabled()).isTrue();
    }
}
