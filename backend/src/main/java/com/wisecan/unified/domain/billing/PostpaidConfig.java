package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 후불 모델 설정 — 05_DATA_MODEL §7.4.
 * 사업자(COMPANY) 한정, 회사당 0~1 행.
 * 운영자 심사 후 status=ACTIVE 로 전환되어야 후불 발송이 허용된다.
 * 신용 한도(credit_limit), 보증보험 증권번호(guarantee_insurance_no) 보유.
 * 02_FEATURE_SPEC §10.3 참조.
 */
@Entity
@Table(name = "postpaid_config",
        uniqueConstraints = @UniqueConstraint(name = "uq_postpaid_config_company", columnNames = "company_id"))
@Getter
@NoArgsConstructor
public class PostpaidConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 회사 ID (COMPANY.id 참조 — stub) */
    @Column(nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostpaidStatus status;

    /** 신용 한도 (원화 정수, null = 심사 전) */
    @Column
    private Long creditLimit;

    /** 정산 주기 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostpaidBillingCycle billingCycle;

    /** 보증보험 증권번호 (null = 미등록) */
    @Column(length = 50)
    private String guaranteeInsuranceNo;

    /** 후불 활성화 시각 (운영자 승인 시각) */
    @Column
    private LocalDateTime activatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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
    public PostpaidConfig(Long companyId, PostpaidBillingCycle billingCycle,
                          Long creditLimit, String guaranteeInsuranceNo) {
        this.companyId = companyId;
        this.status = PostpaidStatus.APPLIED;
        this.billingCycle = billingCycle;
        this.creditLimit = creditLimit;
        this.guaranteeInsuranceNo = guaranteeInsuranceNo;
    }

    // ──────────────────────────────────────────
    // 도메인 메서드
    // ──────────────────────────────────────────

    /** 운영자 심사 시작 */
    public void startReview() {
        if (this.status != PostpaidStatus.APPLIED) {
            throw new IllegalStateException("신청(APPLIED) 상태에서만 심사를 시작할 수 있습니다.");
        }
        this.status = PostpaidStatus.UNDER_REVIEW;
    }

    /** 운영자 승인 — 신용 한도·보증보험 확정 */
    public void approve(Long creditLimit, String guaranteeInsuranceNo) {
        if (this.status != PostpaidStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 중(UNDER_REVIEW) 상태에서만 승인할 수 있습니다.");
        }
        this.creditLimit = creditLimit;
        this.guaranteeInsuranceNo = guaranteeInsuranceNo;
        this.status = PostpaidStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    /** 운영자 정지 (연체 또는 제재) */
    public void suspend() {
        this.status = PostpaidStatus.SUSPENDED;
    }

    /** 후불 발송 허용 여부 */
    public boolean isActive() {
        return PostpaidStatus.ACTIVE.equals(this.status);
    }
}
