package com.wisecan.unified.service.billing;

/**
 * 자동충전 결제 실패 이벤트 — W-402.
 *
 * ChargeService 또는 AutoChargeService 에서 발행.
 * AutoChargeFailedListener 가 AFTER_COMMIT 단계에서 회원·운영자 알림 발송.
 *
 * @param memberId    실패 대상 회원 ID
 * @param configId    자동충전 설정 ID
 * @param failReason  PG 실패 사유 (마스킹된 메시지)
 * @param attemptedAmount 시도 충전 금액 (원)
 */
public record AutoChargeFailedEvent(
        Long memberId,
        Long configId,
        String failReason,
        Long attemptedAmount
) {}
