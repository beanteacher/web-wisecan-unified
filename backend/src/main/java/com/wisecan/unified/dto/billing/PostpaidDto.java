package com.wisecan.unified.dto.billing;

import com.wisecan.unified.domain.billing.PostpaidBillingCycle;
import com.wisecan.unified.domain.billing.PostpaidConfig;
import com.wisecan.unified.domain.billing.PostpaidInvoice;
import com.wisecan.unified.domain.billing.PostpaidInvoiceStatus;
import com.wisecan.unified.domain.billing.PostpaidStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * 후불 모델 DTO — W-403.
 * 02_FEATURE_SPEC §10.3 / 05_DATA_MODEL §7.4.
 */
public class PostpaidDto {

    // ──────────────────────────────────────────
    // Request
    // ──────────────────────────────────────────

    /** 후불 신청 요청 */
    public record ApplyRequest(
            @NotNull PostpaidBillingCycle billingCycle,
            String guaranteeInsuranceNo
    ) {}

    /** 운영자 승인 요청 */
    public record ApproveRequest(
            @NotNull @Positive Long creditLimit,
            @NotNull String guaranteeInsuranceNo
    ) {}

    /** 청구서 결제 요청 */
    public record PayInvoiceRequest(
            @NotNull @Positive Long amount
    ) {}

    /** 청구서 발행 요청 (운영자 또는 배치) */
    public record IssueInvoiceRequest(
            @NotNull Long postpaidConfigId,
            @NotNull String periodLabel,
            @NotNull @Positive Long totalAmount,
            @NotNull LocalDateTime dueAt
    ) {}

    // ──────────────────────────────────────────
    // Response
    // ──────────────────────────────────────────

    /** 후불 설정 응답 */
    public record ConfigResponse(
            Long id,
            Long companyId,
            PostpaidStatus status,
            Long creditLimit,
            PostpaidBillingCycle billingCycle,
            String guaranteeInsuranceNo,
            LocalDateTime activatedAt,
            LocalDateTime createdAt
    ) {
        public static ConfigResponse from(PostpaidConfig config) {
            return new ConfigResponse(
                    config.getId(),
                    config.getCompanyId(),
                    config.getStatus(),
                    config.getCreditLimit(),
                    config.getBillingCycle(),
                    config.getGuaranteeInsuranceNo(),
                    config.getActivatedAt(),
                    config.getCreatedAt()
            );
        }
    }

    /** 청구서 응답 */
    public record InvoiceResponse(
            Long id,
            Long postpaidConfigId,
            String periodLabel,
            Long totalAmount,
            Long paidAmount,
            Long remainingAmount,
            PostpaidInvoiceStatus status,
            LocalDateTime issuedAt,
            LocalDateTime dueAt,
            LocalDateTime paidAt
    ) {
        public static InvoiceResponse from(PostpaidInvoice invoice) {
            return new InvoiceResponse(
                    invoice.getId(),
                    invoice.getPostpaidConfigId(),
                    invoice.getPeriodLabel(),
                    invoice.getTotalAmount(),
                    invoice.getPaidAmount(),
                    invoice.remainingAmount(),
                    invoice.getStatus(),
                    invoice.getIssuedAt(),
                    invoice.getDueAt(),
                    invoice.getPaidAt()
            );
        }
    }
}
