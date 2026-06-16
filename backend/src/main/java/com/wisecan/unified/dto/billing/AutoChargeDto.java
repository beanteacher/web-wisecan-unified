package com.wisecan.unified.dto.billing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 자동충전 설정 DTO — inner record 방식 (W-402).
 *
 * 01. ActivateRequest  — POST /billing/auto-charge (활성화·갱신)
 * 02. Response         — 자동충전 설정 응답
 */
public class AutoChargeDto {

    /** 자동충전 활성화·갱신 요청 */
    public record ActivateRequest(
            @NotNull(message = "결제수단 ID는 필수입니다")
            Long paymentMethodId,

            @NotNull(message = "임계 잔액은 필수입니다")
            @Min(value = 0, message = "임계 잔액은 0원 이상이어야 합니다")
            Long thresholdAmount,

            @NotNull(message = "자동충전 금액은 필수입니다")
            @Min(value = 1000, message = "최소 자동충전 금액은 1,000원입니다")
            Long chargeAmount,

            /**
             * 30일 제한 적용 여부.
             * true 이면 만료일 = 오늘 + 30일, false 이면 무기한(만료 없음).
             */
            boolean apply30DayLimit
    ) {}

    /** 자동충전 설정 응답 */
    public record Response(
            Long id,
            Long memberId,
            Long paymentMethodId,
            Long thresholdAmount,
            Long chargeAmount,
            boolean enabled,
            LocalDate expiresAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
