package com.wisecan.unified.domain.sendernumber;

/**
 * 발신번호 등록 사건 카탈로그.
 *
 * REGISTERED           — 즉시 등록 확정 (SELF_MOBILE / SELF_LANDLINE)
 * REVIEW_APPROVED      — 운영자 심사 승인 (EMPLOYEE / CORP_REP)
 * REVIEW_REJECTED      — 운영자 반려
 * RESUBMIT_REQUESTED   — 보완 서류 재제출 요청
 * DELETED              — 삭제 (직접 삭제 또는 연쇄 삭제)
 * FRAUD_FLAGGED        — 위조 의심 플래그
 */
public enum CallbackLogEvent {
    REGISTERED,
    REVIEW_APPROVED,
    REVIEW_REJECTED,
    RESUBMIT_REQUESTED,
    DELETED,
    FRAUD_FLAGGED
}
