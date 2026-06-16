package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMethodTest {

    @Test
    @DisplayName("빌더 기본값 — activeYn=Y, defaultYn=N")
    void builder_defaults_activeYActive_defaultNone() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .build();

        assertThat(pm.isActive()).isTrue();
        assertThat(pm.isDefault()).isFalse();
    }

    @Test
    @DisplayName("deactivate — activeYn=N, pgBillingKey=null")
    void deactivate_setsInactiveAndClearsBillingKey() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-SECRET-KEY")
                .build();

        pm.deactivate();

        assertThat(pm.isActive()).isFalse();
        assertThat(pm.getPgBillingKey()).isNull();
    }

    @Test
    @DisplayName("markDefault / unmarkDefault — defaultYn 토글")
    void markDefault_andUnmark_togglesDefaultYn() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.BANK_TRANSFER)
                .maskedLabel("국민 **** 5678")
                .build();

        pm.markDefault();
        assertThat(pm.isDefault()).isTrue();

        pm.unmarkDefault();
        assertThat(pm.isDefault()).isFalse();
    }
}
