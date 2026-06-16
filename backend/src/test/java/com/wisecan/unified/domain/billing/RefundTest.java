package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RefundTest {

    private Refund buildRefund(boolean hasCashReceipt) {
        return Refund.builder()
                .memberId(100L)
                .chargeBalanceId(1L)
                .amount(20_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(hasCashReceipt)
                .cashReceiptIssueNo(hasCashReceipt ? "CR-2024-001" : null)
                .build();
    }

    @Test
    @DisplayName("초기 상태는 PENDING")
    void initialStatus_isPending() {
        Refund refund = buildRefund(false);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(refund.isPending()).isTrue();
    }

    @Test
    @DisplayName("approve() — APPROVED 상태로 전환, 운영자 정보 저장")
    void approve_changesStatusToApproved() {
        Refund refund = buildRefund(false);
        refund.approve(1L, "정상 처리");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.APPROVED);
        assertThat(refund.isApproved()).isTrue();
        assertThat(refund.getOperatorId()).isEqualTo(1L);
        assertThat(refund.getOperatorMemo()).isEqualTo("정상 처리");
        assertThat(refund.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("reject() — REJECTED 상태로 전환")
    void reject_changesStatusToRejected() {
        Refund refund = buildRefund(false);
        refund.reject(1L, "사용 이력 있음");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(refund.getOperatorMemo()).isEqualTo("사용 이력 있음");
    }

    @Test
    @DisplayName("cancel() — PENDING 상태에서 CANCELLED 전환 성공")
    void cancel_fromPending_succeeds() {
        Refund refund = buildRefund(false);
        refund.cancel();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel() — PENDING 이 아닌 상태에서 호출 시 IllegalStateException")
    void cancel_fromApproved_throwsIllegalStateException() {
        Refund refund = buildRefund(false);
        refund.approve(1L, "승인");

        assertThatThrownBy(refund::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("처리 중이거나 완료된 환불");
    }

    @Test
    @DisplayName("markCashReceiptCancelled() — cashReceiptCancelled true 로 변경")
    void markCashReceiptCancelled_setsFlag() {
        Refund refund = buildRefund(true);
        assertThat(refund.isCashReceiptCancelled()).isFalse();

        refund.markCashReceiptCancelled();
        assertThat(refund.isCashReceiptCancelled()).isTrue();
    }
}
