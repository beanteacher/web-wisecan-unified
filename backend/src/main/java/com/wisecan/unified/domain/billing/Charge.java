package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 충전 거래 — 05_DATA_MODEL §7.1.
 * 회계 진실 원천. 수동/자동 충전 1건당 1행.
 * pg_tx_id UNIQUE — PG 거래번호 멱등성 보장.
 * CHARGE_BALANCE 의 부모.
 */
@Entity
@Table(name = "charge",
       uniqueConstraints = @UniqueConstraint(name = "uq_charge_pg_tx_id", columnNames = "pg_tx_id"))
@Getter
@NoArgsConstructor
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long paymentMethodId;

    /** nullable — AUTO 충전 시 AUTO_CHARGE_CONFIG id */
    @Column
    private Long triggerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeType chargeType;

    /** 충전 금액 (원화 정수) */
    @Column(nullable = false)
    private Long amount;

    /** PG 거래번호 — UNIQUE, 멱등성 보장 */
    @Column(name = "pg_tx_id", nullable = false, length = 64)
    private String pgTxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(length = 500)
    private String failReason;

    @Column
    private LocalDateTime paidAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Charge(Long memberId, Long paymentMethodId, Long triggerId,
                  ChargeType chargeType, Long amount, String pgTxId) {
        this.memberId = memberId;
        this.paymentMethodId = paymentMethodId;
        this.triggerId = triggerId;
        this.chargeType = chargeType;
        this.amount = amount;
        this.pgTxId = pgTxId;
        this.status = ChargeStatus.REQUESTED;
    }

    /** PG 결제 성공 확정 */
    public void markSuccess() {
        this.status = ChargeStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    /** PG 결제 실패 */
    public void markFailed(String reason) {
        this.status = ChargeStatus.FAILED;
        this.failReason = reason;
    }

    /** 충전 취소 */
    public void cancel() {
        this.status = ChargeStatus.CANCELLED;
    }

    public boolean isSuccess() {
        return ChargeStatus.SUCCESS.equals(this.status);
    }
}
