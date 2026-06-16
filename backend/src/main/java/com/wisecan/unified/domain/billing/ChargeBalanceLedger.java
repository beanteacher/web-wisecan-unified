package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 충전 잔액 변동 원장 (append-only) — 05_DATA_MODEL §7.2.
 * 잔액 변동 사유마다 1행 적재. 절대 수정/삭제 없음.
 * amount 양수 = 적립, 음수 = 차감.
 * SEND 이유의 amount 합산 = 매출 인식액.
 */
@Entity
@Table(name = "charge_balance_ledger")
@Getter
@NoArgsConstructor
public class ChargeBalanceLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chargeBalanceId;

    /** nullable — SEND/EXPIRE/AUTO-ADJUST 는 operatorId 없음 */
    @Column
    private Long operatorId;

    /** 양수 = 적립, 음수 = 차감 */
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BalanceLedgerReason reason;

    /** nullable — SEND 이유일 때 외부 발송 ID (멱등성) */
    @Column(length = 64)
    private String externalSendId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }

    @Builder
    public ChargeBalanceLedger(Long chargeBalanceId, Long operatorId, Long amount,
                               BalanceLedgerReason reason, String externalSendId) {
        this.chargeBalanceId = chargeBalanceId;
        this.operatorId = operatorId;
        this.amount = amount;
        this.reason = reason;
        this.externalSendId = externalSendId;
    }
}
