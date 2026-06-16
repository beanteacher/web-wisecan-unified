package com.wisecan.unified.dto.admin;

import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.domain.billing.RefundStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자 재무 관리 DTO — §12.5.
 *
 * 01. CashAdjustRequest   — POST /admin/billing/cash-adjust  캐시 강제 적립/차감
 * 02. CashAdjustResponse  — 캐시 조정 결과
 * 03. RefundSummary       — 환불 목록 항목 (운영자 전체 목록)
 * 04. SettlementReport    — 정산 보고서 요약
 */
public class AdminBillingDto {

    /** 캐시 강제 적립/차감 요청 */
    public record CashAdjustRequest(
            @NotNull(message = "회원 ID는 필수입니다")
            Long memberId,

            /**
             * 양수 = 적립, 음수 = 차감.
             * M-2 리뷰 반영: primitive long 은 @NotNull 이 동작하지 않으므로 Long 래퍼 사용.
             * 0 금액 방지는 서비스(AdminBillingService.adjustCash) 에서 검증한다.
             */
            @NotNull(message = "조정 금액은 필수입니다")
            Long amount,

            @NotBlank(message = "조정 사유는 필수입니다")
            String reason
    ) {}

    /** 캐시 조정 결과 */
    public record CashAdjustResponse(
            Long memberId,
            long adjustedAmount,
            String reason,
            Long operatorId,
            LocalDateTime adjustedAt
    ) {}

    /** 환불 목록 항목 (운영자 전체 조회) */
    public record RefundSummary(
            Long refundId,
            Long memberId,
            String memberEmail,
            Long amount,
            PaymentMethodType methodType,
            RefundStatus status,
            boolean hasCashReceipt,
            LocalDateTime requestedAt,
            LocalDateTime processedAt
    ) {}

    /** 정산 보고서 — 기간별 집계 */
    public record SettlementReport(
            String periodStart,
            String periodEnd,
            long totalCharged,
            long totalRefunded,
            long totalSent,
            long totalDeducted,
            long netBalance,
            List<SettlementRow> rows
    ) {}

    /** 정산 보고서 행 (회원별) */
    public record SettlementRow(
            Long memberId,
            String memberEmail,
            long charged,
            long refunded,
            long deducted,
            long remainingBalance
    ) {}
}
