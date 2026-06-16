package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 충전 잔액 (예수금) — 05_DATA_MODEL §7.2.
 * 충전 1건당 잔액 1행. 결제수단별 분리 보관.
 * expires_at = charged_at + 5년 (자동 소멸 대상).
 * 발송 차감 시 FIFO + 만료 임박 우선 (ORDER BY expires_at ASC, charged_at ASC).
 * 잔액 합산 캐시: Redis balance:{memberId} (TTL 30s, write-around).
 */
@Entity
@Table(name = "charge_balance")
@Getter
@NoArgsConstructor
public class ChargeBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long chargeId;

    @Column(nullable = false)
    private Long memberId;

    /** 결제수단 사본 — 환불 라우팅 시 사용. CHARGE 삭제 후에도 추적 가능 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethodType methodType;

    @Column(nullable = false)
    private Long amountInitial;

    @Column(nullable = false)
    private Long amountRemaining;

    @Column(nullable = false, updatable = false)
    private LocalDateTime chargedAt;

    /** 충전 + 5년 */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.chargedAt = LocalDateTime.now();
    }

    @Builder
    public ChargeBalance(Long chargeId, Long memberId, PaymentMethodType methodType,
                         Long amountInitial, LocalDateTime expiresAt) {
        this.chargeId = chargeId;
        this.memberId = memberId;
        this.methodType = methodType;
        this.amountInitial = amountInitial;
        this.amountRemaining = amountInitial;
        this.expiresAt = expiresAt;
    }

    /**
     * 잔액 차감 — 요청량만큼 감소.
     * 실제 차감 가능 금액(min(amountRemaining, requested))을 반환.
     */
    public long deduct(long requested) {
        long deductible = Math.min(this.amountRemaining, requested);
        this.amountRemaining -= deductible;
        return deductible;
    }

    /** 잔액 증가 (REVERT 보상 시) */
    public void credit(long amount) {
        this.amountRemaining += amount;
    }

    /** 만료 소멸 처리 */
    public void expire() {
        this.amountRemaining = 0L;
    }

    public boolean hasBalance() {
        return this.amountRemaining > 0;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
