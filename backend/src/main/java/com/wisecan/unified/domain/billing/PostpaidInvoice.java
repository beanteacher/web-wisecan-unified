package com.wisecan.unified.domain.billing;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 후불 청구서 — 05_DATA_MODEL §7.4.
 * 정산 주기(MONTHLY / BIWEEKLY)마다 1행 발행.
 * status=OVERDUE 가 되면 PostpaidBlockGate 가 발송을 차단한다.
 * 02_FEATURE_SPEC §10.3 참조.
 */
@Entity
@Table(name = "postpaid_invoice",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_postpaid_invoice_config_period",
                columnNames = {"postpaid_config_id", "period_label"}))
@Getter
@NoArgsConstructor
public class PostpaidInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postpaid_config_id", nullable = false)
    private Long postpaidConfigId;

    /** 정산 기간 레이블 — "YYYY-MM" 또는 "YYYY-MM-W2" (격주) */
    @Column(nullable = false, length = 20)
    private String periodLabel;

    /** 청구 금액 (원화 정수) */
    @Column(nullable = false)
    private Long totalAmount;

    /** 납부 금액 (부분 납부 지원) */
    @Column(nullable = false)
    private Long paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostpaidInvoiceStatus status;

    /** 청구서 발행 시각 */
    @Column(nullable = false)
    private LocalDateTime issuedAt;

    /** 납부 기한 */
    @Column(nullable = false)
    private LocalDateTime dueAt;

    /** 완납 시각 (null = 미납) */
    @Column
    private LocalDateTime paidAt;

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
    public PostpaidInvoice(Long postpaidConfigId, String periodLabel,
                           Long totalAmount, LocalDateTime issuedAt, LocalDateTime dueAt) {
        this.postpaidConfigId = postpaidConfigId;
        this.periodLabel = periodLabel;
        this.totalAmount = totalAmount;
        this.paidAmount = 0L;
        this.status = PostpaidInvoiceStatus.ISSUED;
        this.issuedAt = issuedAt;
        this.dueAt = dueAt;
    }

    // ──────────────────────────────────────────
    // 도메인 메서드
    // ──────────────────────────────────────────

    /**
     * 청구서 결제 처리.
     * 완납이면 PAID, 부분 납부이면 ISSUED 유지.
     *
     * @param amount 납부 금액
     */
    public void pay(long amount) {
        if (this.status == PostpaidInvoiceStatus.PAID) {
            throw new IllegalStateException("이미 완납된 청구서입니다.");
        }
        if (this.status == PostpaidInvoiceStatus.CANCELLED) {
            throw new IllegalStateException("취소된 청구서는 결제할 수 없습니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("납부 금액은 0보다 커야 합니다.");
        }

        this.paidAmount = Math.min(this.paidAmount + amount, this.totalAmount);

        if (this.paidAmount >= this.totalAmount) {
            this.status = PostpaidInvoiceStatus.PAID;
            this.paidAt = LocalDateTime.now();
        }
        // 연체 상태에서 완납 시에도 PAID 로 전환 → PostpaidBlockGate 차단 해제
    }

    /** 납부 기한 경과 시 연체 처리 (배치에서 호출) */
    public void markOverdue() {
        if (this.status == PostpaidInvoiceStatus.ISSUED) {
            this.status = PostpaidInvoiceStatus.OVERDUE;
        }
    }

    /** 운영자 취소 */
    public void cancel() {
        if (this.status == PostpaidInvoiceStatus.PAID) {
            throw new IllegalStateException("완납된 청구서는 취소할 수 없습니다.");
        }
        this.status = PostpaidInvoiceStatus.CANCELLED;
    }

    /** 잔여 미납액 */
    public long remainingAmount() {
        return this.totalAmount - this.paidAmount;
    }

    public boolean isOverdue() {
        return PostpaidInvoiceStatus.OVERDUE.equals(this.status);
    }

    public boolean isPaid() {
        return PostpaidInvoiceStatus.PAID.equals(this.status);
    }
}
