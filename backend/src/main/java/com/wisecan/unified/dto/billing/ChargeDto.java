package com.wisecan.unified.dto.billing;

import com.wisecan.unified.domain.billing.ChargeStatus;
import com.wisecan.unified.domain.billing.ChargeType;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 충전 관련 DTO — inner record 방식.
 * 01. CreateRequest  — POST /billing/charge 요청
 * 02. Response       — 충전 결과 응답
 * 03. BalanceResponse — 잔액 조회 응답
 */
public class ChargeDto {

    /** 충전 요청 */
    public record CreateRequest(
            @NotNull(message = "결제수단 ID는 필수입니다")
            Long paymentMethodId,

            @NotNull(message = "충전 금액은 필수입니다")
            @Min(value = 1000, message = "최소 충전 금액은 1,000원입니다")
            Long amount
    ) {}

    /** 충전 결과 응답 */
    public record Response(
            Long chargeId,
            Long memberId,
            PaymentMethodType methodType,
            Long amount,
            ChargeType chargeType,
            ChargeStatus status,
            String pgTxId,
            LocalDateTime paidAt,
            LocalDateTime createdAt
    ) {}

    /** 잔액 조회 응답 */
    public record BalanceResponse(
            Long memberId,
            Long totalBalance,
            boolean fromCache
    ) {}

    /** 결제수단 등록 요청 */
    public record RegisterPaymentMethodRequest(
            @NotNull(message = "결제수단 유형은 필수입니다")
            PaymentMethodType methodType,

            String pgBillingKey,

            @NotNull(message = "마스킹 라벨은 필수입니다")
            String maskedLabel,

            boolean setAsDefault
    ) {}

    /** 결제수단 응답 */
    public record PaymentMethodResponse(
            Long id,
            PaymentMethodType methodType,
            String maskedLabel,
            boolean isDefault,
            boolean isActive,
            LocalDateTime registeredAt,
            LocalDateTime expiresAt
    ) {}
}
