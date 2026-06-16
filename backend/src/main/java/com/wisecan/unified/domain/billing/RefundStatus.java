package com.wisecan.unified.domain.billing;

/**
 * 환불 상태 — 02_FEATURE_SPEC §10.4.
 * PENDING   : 환불 신청 완료, 운영자 검토 대기
 * APPROVED  : 운영자 승인 완료, PG 환불 처리 중
 * REJECTED  : 운영자 반려
 * CANCELLED : 처리 전 회원이 직접 취소
 * COMPLETED : PG 환불 완료
 */
public enum RefundStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED
}
