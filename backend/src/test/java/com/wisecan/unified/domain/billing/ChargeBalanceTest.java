package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChargeBalanceTest {

    private ChargeBalance buildBalance(long amount) {
        return ChargeBalance.builder()
                .chargeId(1L)
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(amount)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .build();
    }

    @Test
    @DisplayName("deduct — 요청액 전액 차감 가능 시 amountRemaining 감소")
    void deduct_fullAmount_reducesRemaining() {
        ChargeBalance cb = buildBalance(50_000L);

        long deducted = cb.deduct(30_000L);

        assertThat(deducted).isEqualTo(30_000L);
        assertThat(cb.getAmountRemaining()).isEqualTo(20_000L);
    }

    @Test
    @DisplayName("deduct — 요청액이 잔액 초과 시 잔액만큼만 차감")
    void deduct_exceedsRemaining_deductsOnlyAvailable() {
        ChargeBalance cb = buildBalance(10_000L);

        long deducted = cb.deduct(20_000L);

        assertThat(deducted).isEqualTo(10_000L);
        assertThat(cb.getAmountRemaining()).isEqualTo(0L);
    }

    @Test
    @DisplayName("credit — 보상 복구 시 amountRemaining 증가")
    void credit_addsToRemaining() {
        ChargeBalance cb = buildBalance(10_000L);
        cb.deduct(10_000L);

        cb.credit(5_000L);

        assertThat(cb.getAmountRemaining()).isEqualTo(5_000L);
    }

    @Test
    @DisplayName("expire — amountRemaining 0으로 소멸")
    void expire_setsRemainingToZero() {
        ChargeBalance cb = buildBalance(50_000L);

        cb.expire();

        assertThat(cb.getAmountRemaining()).isEqualTo(0L);
        assertThat(cb.hasBalance()).isFalse();
    }

    @Test
    @DisplayName("isExpired — 만료일 경과 시 true")
    void isExpired_pastExpiresAt_returnsTrue() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(1000L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(cb.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired — 만료일 미경과 시 false")
    void isExpired_futureExpiresAt_returnsFalse() {
        ChargeBalance cb = buildBalance(1000L);

        assertThat(cb.isExpired()).isFalse();
    }
}
