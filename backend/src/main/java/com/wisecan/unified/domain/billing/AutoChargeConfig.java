package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 자동충전 설정 — 05_DATA_MODEL §7.4 (W-402).
 *
 * 잔액이 thresholdAmount 이하로 떨어지면 chargeAmount 만큼 자동 결제.
 * - PG 빌링키를 보유한 결제수단(paymentMethodId)으로 정기결제.
 * - 30일 제한 옵션: expiresAt 이 NULL 이 아니면 만료 후 자동 비활성화.
 * - enabled=false 이면 트리거 대상에서 제외.
 */
@Entity
@Table(name = "auto_charge_config",
       uniqueConstraints = @UniqueConstraint(name = "uq_acc_member", columnNames = "member_id"))
@Getter
@NoArgsConstructor
public class AutoChargeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 설정 소유 회원 — 1회원 1설정 (UNIQUE) */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 자동충전에 사용할 결제수단 (pgBillingKey 보유 필수) */
    @Column(nullable = false)
    private Long paymentMethodId;

    /** 잔액 임계치 (원) — 이 금액 이하가 되면 충전 트리거 */
    @Column(nullable = false)
    private Long thresholdAmount;

    /** 1회 자동충전 금액 (원, 최소 1,000) */
    @Column(nullable = false)
    private Long chargeAmount;

    /** 활성화 여부 */
    @Column(nullable = false, length = 1)
    private String enabledYn = "Y";

    /**
     * 30일 제한 만료일 — NULL 이면 무기한.
     * 활성화 시 now + 30일로 설정; null 전달 시 무기한.
     */
    @Column
    private LocalDate expiresAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public AutoChargeConfig(Long memberId, Long paymentMethodId,
                            Long thresholdAmount, Long chargeAmount,
                            String enabledYn, LocalDate expiresAt) {
        this.memberId = memberId;
        this.paymentMethodId = paymentMethodId;
        this.thresholdAmount = thresholdAmount;
        this.chargeAmount = chargeAmount;
        this.enabledYn = enabledYn != null ? enabledYn : "Y";
        this.expiresAt = expiresAt;
    }

    /** 설정 갱신 (활성화 시 30일 제한 재설정 포함) */
    public void update(Long paymentMethodId, Long thresholdAmount,
                       Long chargeAmount, LocalDate expiresAt) {
        this.paymentMethodId = paymentMethodId;
        this.thresholdAmount = thresholdAmount;
        this.chargeAmount = chargeAmount;
        this.expiresAt = expiresAt;
        this.enabledYn = "Y";
    }

    /** 자동충전 활성화 */
    public void enable() {
        this.enabledYn = "Y";
    }

    /** 자동충전 비활성화 (해지) */
    public void disable() {
        this.enabledYn = "N";
    }

    /** 만료일 초과 시 자동 비활성화 */
    public void disableIfExpired() {
        if (isExpired()) {
            this.enabledYn = "N";
        }
    }

    public boolean isEnabled() {
        return "Y".equals(this.enabledYn);
    }

    /**
     * 만료 여부 — expiresAt 이 설정되어 있고 오늘 날짜를 지난 경우.
     * expiresAt == null 이면 무기한(만료 없음).
     */
    public boolean isExpired() {
        return this.expiresAt != null && LocalDate.now().isAfter(this.expiresAt);
    }

    /**
     * 임계 도달 여부 — 현재 잔액이 임계치 이하이고 설정이 활성 상태이며 만료되지 않음.
     *
     * @param currentBalance 현재 잔액 (원)
     */
    public boolean shouldTrigger(long currentBalance) {
        return isEnabled() && !isExpired() && currentBalance <= this.thresholdAmount;
    }
}
