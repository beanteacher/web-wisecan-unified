package com.wisecan.unified.service.billing;

import java.util.List;

/**
 * 잔액 부족 분기 결과 — W-405.
 *
 * 02_FEATURE_SPEC §11.1 4가지 분기를 타입으로 표현한다.
 *
 * <pre>
 *   AUTO_CHARGED  — 자동결제 성공 → 발송 계속
 *   POSTPAID      — 후불 처리 → 발송 계속
 *   PARTIAL       — 부분 발송 → HTTP 207
 *   CANCELLED     — 전체 취소 → HTTP 402
 * </pre>
 */
public sealed interface BalanceBranchResult
        permits BalanceBranchResult.AutoCharged,
                BalanceBranchResult.Postpaid,
                BalanceBranchResult.Partial,
                BalanceBranchResult.Cancelled {

    /** 자동결제 성공 — 충전 후 발송 계속 */
    record AutoCharged(long chargedAmount) implements BalanceBranchResult {}

    /** 후불 처리 — 발송 계속 (청구서 누적) */
    record Postpaid(long deferredAmount) implements BalanceBranchResult {}

    /**
     * 부분 발송 — HTTP 207 Multi-Status.
     *
     * @param acceptedCount    적재 승인된 수신자 수
     * @param rejectedCount    잔액 부족으로 거부된 수신자 수
     * @param rejectedNumbers  거부된 수신자 번호 목록
     * @param shortfall        부족 금액 (원)
     */
    record Partial(
            int acceptedCount,
            int rejectedCount,
            List<String> rejectedNumbers,
            long shortfall
    ) implements BalanceBranchResult {}

    /**
     * 전체 취소 — 회원이 취소를 선택.
     *
     * @param shortfall 부족 금액 (원)
     */
    record Cancelled(long shortfall) implements BalanceBranchResult {}
}
