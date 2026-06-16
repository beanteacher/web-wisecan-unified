package com.wisecan.unified.dto.billing;

import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.domain.billing.RefundStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 환불 관련 DTO — 02_FEATURE_SPEC §10.4.
 *
 * 01. CreateRequest     — POST /billing/refund 환불 신청
 * 02. Response          — 환불 상태 응답
 * 03. ApproveRequest    — PUT /admin/billing/refund/{id}/approve 운영자 승인
 * 04. RejectRequest     — PUT /admin/billing/refund/{id}/reject 운영자 반려
 */
public class RefundDto {

    /** 환불 신청 */
    public record CreateRequest(
            @NotNull(message = "환불 대상 잔액 ID는 필수입니다")
            Long chargeBalanceId,

            @NotNull(message = "환불 금액은 필수입니다")
            @Min(value = 1, message = "환불 금액은 1원 이상이어야 합니다")
            Long amount,

            /** 현금영수증 발급 여부 */
            boolean hasCashReceipt,

            /** 현금영수증 국세청 발행 번호 (hasCashReceipt=true 시 필수) */
            String cashReceiptIssueNo
    ) {}

    /** 환불 상태 응답 */
    public record Response(
            Long refundId,
            Long memberId,
            Long chargeBalanceId,
            Long amount,
            PaymentMethodType methodType,
            RefundStatus status,
            boolean hasCashReceipt,
            boolean cashReceiptCancelled,
            String operatorMemo,
            LocalDateTime requestedAt,
            LocalDateTime processedAt
    ) {}

    /** 운영자 승인 요청 */
    public record ApproveRequest(
            String memo
    ) {}

    /** 운영자 반려 요청 */
    public record RejectRequest(
            @NotNull(message = "반려 사유는 필수입니다")
            String memo
    ) {}
}
