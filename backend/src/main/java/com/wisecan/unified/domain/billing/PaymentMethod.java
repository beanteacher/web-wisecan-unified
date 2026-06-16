package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 등록 결제수단 — 05_DATA_MODEL §7.1.
 * 7종 결제수단 + PG 빌키(자동충전용).
 * 무통장입금 미지원.
 */
@Entity
@Table(name = "payment_method")
@Getter
@NoArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethodType methodType;

    /** PG 빌키 — 자동충전 정기결제용. 수동 충전 수단은 NULL */
    @Column(length = 64)
    private String pgBillingKey;

    /** 카드 4자리 또는 계좌 마스킹 표시 라벨 */
    @Column(length = 50)
    private String maskedLabel;

    @Column(nullable = false, length = 1)
    private String defaultYn = "N";

    @Column(nullable = false, length = 1)
    private String activeYn = "Y";

    @Column(updatable = false)
    private LocalDateTime registeredAt;

    @Column
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }

    @Builder
    public PaymentMethod(Long memberId, PaymentMethodType methodType,
                         String pgBillingKey, String maskedLabel,
                         String defaultYn, LocalDateTime expiresAt) {
        this.memberId = memberId;
        this.methodType = methodType;
        this.pgBillingKey = pgBillingKey;
        this.maskedLabel = maskedLabel;
        this.defaultYn = defaultYn != null ? defaultYn : "N";
        this.activeYn = "Y";
        this.expiresAt = expiresAt;
    }

    public void deactivate() {
        this.activeYn = "N";
        this.pgBillingKey = null;
    }

    public void markDefault() {
        this.defaultYn = "Y";
    }

    public void unmarkDefault() {
        this.defaultYn = "N";
    }

    public boolean isActive() {
        return "Y".equals(this.activeYn);
    }

    public boolean isDefault() {
        return "Y".equals(this.defaultYn);
    }
}
