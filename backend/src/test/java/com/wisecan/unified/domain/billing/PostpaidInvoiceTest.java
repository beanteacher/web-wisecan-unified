package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PostpaidInvoice — 청구서 도메인 단위 테스트")
class PostpaidInvoiceTest {

    private PostpaidInvoice buildInvoice(long totalAmount) {
        return PostpaidInvoice.builder()
                .postpaidConfigId(1L)
                .periodLabel("2026-06")
                .totalAmount(totalAmount)
                .issuedAt(LocalDateTime.now())
                .dueAt(LocalDateTime.now().plusDays(14))
                .build();
    }

    // ──────────────────────────────────────────
    // 초기 상태
    // ──────────────────────────────────────────

    @Test
    @DisplayName("생성 시 status=ISSUED, paidAmount=0")
    void create_initialState() {
        PostpaidInvoice inv = buildInvoice(100_000L);

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.ISSUED);
        assertThat(inv.getPaidAmount()).isEqualTo(0L);
        assertThat(inv.remainingAmount()).isEqualTo(100_000L);
        assertThat(inv.isPaid()).isFalse();
        assertThat(inv.isOverdue()).isFalse();
    }

    // ──────────────────────────────────────────
    // 결제 (pay)
    // ──────────────────────────────────────────

    @Test
    @DisplayName("완납 — PAID 전환, paidAt 설정")
    void pay_fullAmount_becomesPaid() {
        PostpaidInvoice inv = buildInvoice(50_000L);

        inv.pay(50_000L);

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.PAID);
        assertThat(inv.isPaid()).isTrue();
        assertThat(inv.getPaidAt()).isNotNull();
        assertThat(inv.remainingAmount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("초과 납부 — totalAmount 에 클램프, PAID 전환")
    void pay_overAmount_clampedAndPaid() {
        PostpaidInvoice inv = buildInvoice(30_000L);

        inv.pay(99_999L);

        assertThat(inv.getPaidAmount()).isEqualTo(30_000L);
        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.PAID);
    }

    @Test
    @DisplayName("부분 납부 — ISSUED 유지")
    void pay_partial_remainsIssued() {
        PostpaidInvoice inv = buildInvoice(100_000L);

        inv.pay(40_000L);

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.ISSUED);
        assertThat(inv.getPaidAmount()).isEqualTo(40_000L);
        assertThat(inv.remainingAmount()).isEqualTo(60_000L);
        assertThat(inv.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("이미 PAID 이면 pay() 예외")
    void pay_alreadyPaid_throws() {
        PostpaidInvoice inv = buildInvoice(10_000L);
        inv.pay(10_000L);

        assertThatThrownBy(() -> inv.pay(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완납");
    }

    @Test
    @DisplayName("CANCELLED 이면 pay() 예외")
    void pay_cancelled_throws() {
        PostpaidInvoice inv = buildInvoice(10_000L);
        inv.cancel();

        assertThatThrownBy(() -> inv.pay(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("취소");
    }

    @Test
    @DisplayName("금액 0 이하이면 pay() 예외")
    void pay_zeroAmount_throws() {
        PostpaidInvoice inv = buildInvoice(10_000L);

        assertThatThrownBy(() -> inv.pay(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ──────────────────────────────────────────
    // 연체 처리 (markOverdue)
    // ──────────────────────────────────────────

    @Test
    @DisplayName("ISSUED → markOverdue() → OVERDUE")
    void markOverdue_fromIssued_becomesOverdue() {
        PostpaidInvoice inv = buildInvoice(20_000L);

        inv.markOverdue();

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.OVERDUE);
        assertThat(inv.isOverdue()).isTrue();
    }

    @Test
    @DisplayName("PAID 이면 markOverdue() 무시")
    void markOverdue_paid_noChange() {
        PostpaidInvoice inv = buildInvoice(20_000L);
        inv.pay(20_000L);

        inv.markOverdue();

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.PAID);
    }

    @Test
    @DisplayName("연체 상태에서 완납 — PAID 전환 (발송 차단 해제)")
    void pay_afterOverdue_becomesPaid() {
        PostpaidInvoice inv = buildInvoice(60_000L);
        inv.markOverdue();

        inv.pay(60_000L);

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.PAID);
        assertThat(inv.isOverdue()).isFalse();
    }

    // ──────────────────────────────────────────
    // 취소 (cancel)
    // ──────────────────────────────────────────

    @Test
    @DisplayName("ISSUED → cancel() → CANCELLED")
    void cancel_fromIssued_becomesCancelled() {
        PostpaidInvoice inv = buildInvoice(10_000L);

        inv.cancel();

        assertThat(inv.getStatus()).isEqualTo(PostpaidInvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("PAID 이면 cancel() 예외")
    void cancel_paid_throws() {
        PostpaidInvoice inv = buildInvoice(10_000L);
        inv.pay(10_000L);

        assertThatThrownBy(() -> inv.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완납");
    }
}
