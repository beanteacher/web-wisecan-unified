package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.*;
import com.wisecan.unified.dto.billing.RefundDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.RefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock RefundRepository refundRepository;
    @Mock ChargeBalanceRepository chargeBalanceRepository;
    @Mock ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    @Mock CashReceiptPort cashReceiptPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RefundService refundService;

    // ──────────────────────────────────────────────
    // 환불 신청 (requestRefund)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("환불 신청 — 정상 케이스: PENDING 상태로 저장")
    void requestRefund_success_savedAsPending() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));
        given(refundRepository.existsByChargeBalanceIdAndStatusIn(eq(1L), anyList())).willReturn(false);

        Refund saved = Refund.builder()
                .memberId(100L).chargeBalanceId(1L).amount(30_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false).cashReceiptIssueNo(null)
                .build();
        given(refundRepository.save(any(Refund.class))).willReturn(saved);

        RefundDto.CreateRequest req = new RefundDto.CreateRequest(1L, 30_000L, false, null);
        RefundDto.Response response = refundService.requestRefund(100L, req);

        assertThat(response.status()).isEqualTo(RefundStatus.PENDING);
        assertThat(response.amount()).isEqualTo(30_000L);
        then(refundRepository).should().save(any(Refund.class));
    }

    @Test
    @DisplayName("환불 신청 — 타인의 잔액 신청 시 BillingException")
    void requestRefund_anotherMemberBalance_throwsBillingException() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(999L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));

        RefundDto.CreateRequest req = new RefundDto.CreateRequest(1L, 10_000L, false, null);

        assertThatThrownBy(() -> refundService.requestRefund(100L, req))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("본인의 잔액");
    }

    @Test
    @DisplayName("환불 신청 — 5년 만료 잔액 환불 불가")
    void requestRefund_expiredBalance_throwsBillingException() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));

        RefundDto.CreateRequest req = new RefundDto.CreateRequest(1L, 10_000L, false, null);

        assertThatThrownBy(() -> refundService.requestRefund(100L, req))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("5년");
    }

    @Test
    @DisplayName("환불 신청 — 요청 금액이 잔액 초과 시 BillingException")
    void requestRefund_amountExceedsBalance_throwsBillingException() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(10_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));

        RefundDto.CreateRequest req = new RefundDto.CreateRequest(1L, 20_000L, false, null);

        assertThatThrownBy(() -> refundService.requestRefund(100L, req))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("잔액 초과");
    }

    @Test
    @DisplayName("환불 신청 — 이미 처리 중인 환불이 있으면 BillingException")
    void requestRefund_duplicatePending_throwsBillingException() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));
        given(refundRepository.existsByChargeBalanceIdAndStatusIn(eq(1L), anyList())).willReturn(true);

        RefundDto.CreateRequest req = new RefundDto.CreateRequest(1L, 10_000L, false, null);

        assertThatThrownBy(() -> refundService.requestRefund(100L, req))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("이미 처리 중");
    }

    // ──────────────────────────────────────────────
    // 환불 취소 (cancelRefund)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("환불 취소 — 본인의 PENDING 환불 정상 취소")
    void cancelRefund_success() {
        Refund refund = Refund.builder()
                .memberId(100L).chargeBalanceId(1L).amount(10_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false).cashReceiptIssueNo(null)
                .build();
        given(refundRepository.findById(10L)).willReturn(Optional.of(refund));

        refundService.cancelRefund(100L, 10L);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.CANCELLED);
    }

    @Test
    @DisplayName("환불 취소 — 타인의 환불 취소 시도 BillingException")
    void cancelRefund_anotherMember_throwsBillingException() {
        Refund refund = Refund.builder()
                .memberId(999L).chargeBalanceId(1L).amount(10_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false).cashReceiptIssueNo(null)
                .build();
        given(refundRepository.findById(10L)).willReturn(Optional.of(refund));

        assertThatThrownBy(() -> refundService.cancelRefund(100L, 10L))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("본인의 환불");
    }

    // ──────────────────────────────────────────────
    // 운영자 승인 (approveRefund)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("운영자 승인 — 현금영수증 없는 건: 잔액 차감 + 원장 기록")
    void approveRefund_noCashReceipt_deductsBalance() {
        Refund refund = Refund.builder()
                .memberId(100L).chargeBalanceId(1L).amount(20_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false).cashReceiptIssueNo(null)
                .build();
        given(refundRepository.findById(10L)).willReturn(Optional.of(refund));

        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));

        RefundDto.ApproveRequest req = new RefundDto.ApproveRequest("정상 처리");
        refundService.approveRefund(10L, 1L, req);

        assertThat(refund.isApproved()).isTrue();
        assertThat(cb.getAmountRemaining()).isEqualTo(30_000L);
        then(chargeBalanceLedgerRepository).should().save(any(ChargeBalanceLedger.class));
        then(eventPublisher).should().publishEvent(any(BalanceCacheEvictEvent.class));
        then(cashReceiptPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("운영자 승인 — 현금영수증 있는 건: 마이너스 정정 자동 발행")
    void approveRefund_withCashReceipt_cancelsCashReceipt() {
        Refund refund = Refund.builder()
                .memberId(100L).chargeBalanceId(1L).amount(20_000L)
                .methodType(PaymentMethodType.BANK_TRANSFER)
                .hasCashReceipt(true).cashReceiptIssueNo("CR-2024-001")
                .build();
        given(refundRepository.findById(10L)).willReturn(Optional.of(refund));

        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.BANK_TRANSFER)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(3))
                .build();
        given(chargeBalanceRepository.findById(1L)).willReturn(Optional.of(cb));
        given(cashReceiptPort.cancelCashReceipt("CR-2024-001", 20_000L))
                .willReturn(CashReceiptPort.CashReceiptResult.success("CANCEL-CR-2024-001"));

        RefundDto.ApproveRequest req = new RefundDto.ApproveRequest("정상 처리");
        refundService.approveRefund(10L, 1L, req);

        assertThat(refund.isCashReceiptCancelled()).isTrue();
        then(cashReceiptPort).should().cancelCashReceipt("CR-2024-001", 20_000L);
    }

    @Test
    @DisplayName("운영자 반려 — REJECTED 상태로 전환")
    void rejectRefund_success() {
        Refund refund = Refund.builder()
                .memberId(100L).chargeBalanceId(1L).amount(10_000L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false).cashReceiptIssueNo(null)
                .build();
        given(refundRepository.findById(10L)).willReturn(Optional.of(refund));

        RefundDto.RejectRequest req = new RefundDto.RejectRequest("사용 이력 확인 필요");
        refundService.rejectRefund(10L, 1L, req);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(refund.getOperatorMemo()).isEqualTo("사용 이력 확인 필요");
    }
}
