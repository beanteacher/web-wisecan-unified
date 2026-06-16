package com.wisecan.unified.domain.sendernumber;

/**
 * 발신번호 상태 머신.
 *
 * SUBMITTED    — 등록 신청 접수 (서류 심사형 케이스 초기 상태)
 * UNDER_REVIEW — 운영자 심사 중
 * REGISTERED   — 등록 완료 (발송 가능 상태)
 * REJECTED     — 반려 (재제출 가능)
 * DELETED      — 삭제 (연쇄 삭제 포함, 종료 상태)
 */
public enum CallbackStatus {
    SUBMITTED,
    UNDER_REVIEW,
    REGISTERED,
    REJECTED,
    DELETED
}
