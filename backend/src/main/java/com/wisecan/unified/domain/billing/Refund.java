package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 환불 신청 — 02_FEATURE_SPEC §10.4, 05_DATA_MODEL §7.3.
 *
 * 결제수단별 크레딧 매칭 환불.
 * 5년(chargeBalance.expiresAt) 경과 건은 환불 불가 — 서비스 레이어에서 사전 검증.
 * 현금영수증 발급된 건(hasCashReceipt=true)은 승인 시 마이너스 발행 정정 자동 처리.
 */
@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    /** 환불 대상 ChargeBalance ID */
    @Column(nullable = false)
    private Long chargeBalanceId;

    /** 환불 금액 (원화 정수, amountRemaining 이하) */
    @Column(nullable = false)
    private Long amount;

    /** 결제수단 사본 — 환불 라우팅 시 사용 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethodType methodType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    /** 현금영수증 발급 여부 — true 면 승인 시 마이너스 발행 정정 필요 */
    @Column(nullable = false)
    private boolean hasCashReceipt;

    /** 현금영수증 국세청 발행 번호 (hasCashReceipt=true 일 때 비어 있지 않음) */
    @Column(length = 64)
    private String cashReceiptIssueNo;

    /** 운영자 처리 메모 (승인·반려 사유) */
    @Column(length = 500)
    private String operatorMemo;

    /** 운영자 처리자 ID */
    @Column
    private Long operatorId;

    /** 마이너스 현금영수증 발행 완료 여부 */
    @Column(nullable = false)
    private boolean cashReceiptCancelled;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        this.cashReceiptCancelled = false;
    }

    @Builder
    public Refund(Long memberId, Long chargeBalanceId, Long amount,
                  PaymentMethodType methodType, boolean hasCashReceipt, String cashReceiptIssueNo) {
        this.memberId = memberId;
        this.chargeBalanceId = chargeBalanceId;
        this.amount = amount;
        this.methodType = methodType;
        this.hasCashReceipt = hasCashReceipt;
        this.cashReceiptIssueNo = cashReceiptIssueNo;
        this.status = RefundStatus.PENDING;
        this.cashReceiptCancelled = false;
    }

    /** 운영자 승인 */
    public void approve(Long operatorId, String memo) {
        this.status = RefundStatus.APPROVED;
        this.operatorId = operatorId;
        this.operatorMemo = memo;
        this.processedAt = LocalDateTime.now();
    }

    /** 운영자 반려 */
    public void reject(Long operatorId, String memo) {
        this.status = RefundStatus.REJECTED;
        this.operatorId = operatorId;
        this.operatorMemo = memo;
        this.processedAt = LocalDateTime.now();
    }

    /** 회원 직접 취소 (PENDING 상태에서만 가능) */
    public void cancel() {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException("처리 중이거나 완료된 환불은 취소할 수 없습니다.");
        }
        this.status = RefundStatus.CANCELLED;
    }

    /** PG 환불 완료 확정 */
    public void complete() {
        this.status = RefundStatus.COMPLETED;
    }

    /** 마이너스 현금영수증 발행 완료 표시 */
    public void markCashReceiptCancelled() {
        this.cashReceiptCancelled = true;
    }

    public boolean isPending() {
        return RefundStatus.PENDING.equals(this.status);
    }

    public boolean isApproved() {
        return RefundStatus.APPROVED.equals(this.status);
    }
}
