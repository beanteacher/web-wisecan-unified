package com.wisecan.unified.dto.dispatch;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 잔액 부족 분기 DTO — W-405.
 *
 * 02_FEATURE_SPEC §11.1 분기 평가 요청/응답.
 */
public class BalanceBranchDto {

    // ── 요청 ─────────────────────────────────────────────────────────

    /**
     * 잔액 부족 분기 평가 요청.
     *
     * @param companyId     소속 회사 ID (개인 회원이면 null, 후불 분기 판별용)
     * @param totalCost     총 차감 예정액 (원)
     * @param allRecipients 전체 수신자 번호 목록 (부분 발송 계산용)
     * @param unitCost      건당 단가 (원)
     * @param partialChoice true = 부분 발송, false = 전체 취소 (분기 3·4 해당 시)
     */
    public record EvaluateRequest(
            Long companyId,

            @NotNull(message = "총 차감 예정액은 필수입니다")
            @Min(value = 1, message = "총 차감 예정액은 1원 이상이어야 합니다")
            Long totalCost,

            @NotEmpty(message = "수신자 목록은 필수입니다")
            @Size(max = 100000, message = "수신자는 최대 100,000명입니다")
            List<String> allRecipients,

            @NotNull(message = "건당 단가는 필수입니다")
            @Min(value = 1, message = "건당 단가는 1원 이상이어야 합니다")
            Long unitCost,

            boolean partialChoice
    ) {}

    // ── 응답 ─────────────────────────────────────────────────────────

    /**
     * 분기1 응답 — 자동결제 성공 (HTTP 200).
     *
     * @param chargedAmount 자동결제 충전 금액 (0이면 재조회 시 이미 충분했던 경우)
     */
    public record AutoChargedResponse(
            long chargedAmount
    ) {}

    /**
     * 분기2 응답 — 후불 처리 (HTTP 200).
     *
     * @param deferredAmount 후불 처리 금액 (청구서에 누적됨)
     */
    public record PostpaidResponse(
            long deferredAmount
    ) {}

    /**
     * 분기3 응답 — 부분 발송 (HTTP 207 Multi-Status).
     *
     * 02_FEATURE_SPEC §11.1: 적재 N건의 결과 + 거부 M건의 사유(INSUFFICIENT_BALANCE).
     */
    public record PartialResponse(
            int acceptedCount,
            int rejectedCount,
            List<String> rejectedNumbers,
            String rejectReason,
            long shortfall
    ) {}
}
