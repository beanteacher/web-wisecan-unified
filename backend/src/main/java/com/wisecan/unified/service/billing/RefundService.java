package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.*;
import com.wisecan.unified.dto.billing.RefundDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 환불 서비스 — W-404.
 *
 * 환불 흐름:
 *   1. 회원이 환불 신청 (PENDING)
 *   2. 운영자 승인 (APPROVED) → 잔액 차감 + 원장 기록 + 현금영수증 마이너스 정정(해당 시)
 *   3. 운영자 반려 (REJECTED) 또는 회원 직접 취소 (CANCELLED)
 *
 * 결제수단별 환불 라우팅:
 *   - CREDIT_CARD / DEBIT_CARD : PG 카드 취소 API (현재 Stub)
 *   - BANK_TRANSFER / VIRTUAL_ACCOUNT : 계좌 송금 (현재 Stub)
 *   - MOBILE : 휴대폰 소액결제 취소 (현재 Stub)
 *   - GIFT_CARD / POINT : 원천 복원 (현재 Stub)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final RefundRepository refundRepository;
    private final ChargeBalanceRepository chargeBalanceRepository;
    private final ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    private final CashReceiptPort cashReceiptPort;
    private final ApplicationEventPublisher eventPublisher;

    /** 환불 신청 — POST /billing/refund */
    @Transactional(rollbackFor = Exception.class)
    public RefundDto.Response requestRefund(Long memberId, RefundDto.CreateRequest request) {
        ChargeBalance cb = chargeBalanceRepository.findById(request.chargeBalanceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "충전 잔액을 찾을 수 없습니다: " + request.chargeBalanceId()));

        if (!cb.getMemberId().equals(memberId)) {
            throw new BillingException("본인의 잔액만 환불 신청할 수 있습니다.");
        }

        if (cb.isExpired()) {
            throw new BillingException("5년이 경과한 잔액은 환불이 불가합니다.");
        }

        if (request.amount() > cb.getAmountRemaining()) {
            throw new BillingException(
                    "잔액 초과: 요청 금액(" + request.amount() + ")이 잔액(" + cb.getAmountRemaining() + ")을 초과합니다.");
        }

        boolean duplicateExists = refundRepository.existsByChargeBalanceIdAndStatusIn(
                request.chargeBalanceId(),
                List.of(RefundStatus.PENDING, RefundStatus.APPROVED));
        if (duplicateExists) {
            throw new BillingException("이미 처리 중인 환불 신청이 있습니다.");
        }

        Refund refund = Refund.builder()
                .memberId(memberId)
                .chargeBalanceId(request.chargeBalanceId())
                .amount(request.amount())
                .methodType(cb.getMethodType())
                .hasCashReceipt(request.hasCashReceipt())
                .cashReceiptIssueNo(request.cashReceiptIssueNo())
                .build();
        refund = refundRepository.save(refund);

        log.info("[환불 신청] memberId={} chargeBalanceId={} amount={}", memberId, request.chargeBalanceId(), request.amount());
        return toResponse(refund);
    }

    /** 환불 취소 — DELETE /billing/refund/{refundId} (회원 직접) */
    @Transactional(rollbackFor = Exception.class)
    public void cancelRefund(Long memberId, Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("환불 신청을 찾을 수 없습니다: " + refundId));

        if (!refund.getMemberId().equals(memberId)) {
            throw new BillingException("본인의 환불 신청만 취소할 수 있습니다.");
        }

        refund.cancel();
        log.info("[환불 취소] memberId={} refundId={}", memberId, refundId);
    }

    /** 운영자 승인 — PUT /admin/billing/refund/{refundId}/approve */
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId, Long operatorId, RefundDto.ApproveRequest request) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("환불 신청을 찾을 수 없습니다: " + refundId));

        if (!refund.isPending()) {
            throw new BillingException("PENDING 상태의 환불만 승인할 수 있습니다. 현재 상태: " + refund.getStatus());
        }

        ChargeBalance cb = chargeBalanceRepository.findById(refund.getChargeBalanceId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "충전 잔액을 찾을 수 없습니다: " + refund.getChargeBalanceId()));

        // 잔액 차감 (REFUND 원장 기록)
        long deducted = cb.deduct(refund.getAmount());
        ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                .chargeBalanceId(cb.getId())
                .operatorId(operatorId)
                .amount(-deducted)
                .reason(BalanceLedgerReason.REFUND)
                .build();
        chargeBalanceLedgerRepository.save(ledger);

        // 현금영수증 마이너스 정정 (hasCashReceipt=true 인 경우 자동 발행)
        if (refund.isHasCashReceipt() && refund.getCashReceiptIssueNo() != null) {
            CashReceiptPort.CashReceiptResult result =
                    cashReceiptPort.cancelCashReceipt(refund.getCashReceiptIssueNo(), refund.getAmount());
            if (result.success()) {
                refund.markCashReceiptCancelled();
                log.info("[현금영수증 마이너스 정정] refundId={} cancelIssueNo={}", refundId, result.issueNo());
            } else {
                log.warn("[현금영수증 마이너스 정정 실패] refundId={} reason={}", refundId, result.failReason());
                // 현금영수증 실패는 환불 승인을 막지 않음 — 운영자가 수동 처리
            }
        }

        refund.approve(operatorId, request.memo());
        eventPublisher.publishEvent(new BalanceCacheEvictEvent(refund.getMemberId()));
        log.info("[환불 승인] refundId={} operatorId={} amount={}", refundId, operatorId, refund.getAmount());
    }

    /** 운영자 반려 — PUT /admin/billing/refund/{refundId}/reject */
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long refundId, Long operatorId, RefundDto.RejectRequest request) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("환불 신청을 찾을 수 없습니다: " + refundId));

        if (!refund.isPending()) {
            throw new BillingException("PENDING 상태의 환불만 반려할 수 있습니다. 현재 상태: " + refund.getStatus());
        }

        refund.reject(operatorId, request.memo());
        log.info("[환불 반려] refundId={} operatorId={} memo={}", refundId, operatorId, request.memo());
    }

    /** 회원 환불 목록 조회 — GET /billing/refund */
    @Transactional(readOnly = true)
    public List<RefundDto.Response> listRefunds(Long memberId) {
        return refundRepository.findByMemberIdOrderByRequestedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RefundDto.Response toResponse(Refund refund) {
        return new RefundDto.Response(
                refund.getId(),
                refund.getMemberId(),
                refund.getChargeBalanceId(),
                refund.getAmount(),
                refund.getMethodType(),
                refund.getStatus(),
                refund.isHasCashReceipt(),
                refund.isCashReceiptCancelled(),
                refund.getOperatorMemo(),
                refund.getRequestedAt(),
                refund.getProcessedAt()
        );
    }
}
