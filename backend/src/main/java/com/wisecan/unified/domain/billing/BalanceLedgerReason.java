package com.wisecan.unified.domain.billing;

/**
 * 충전 잔액 변동 사유 — 05_DATA_MODEL §7.2.
 * SEND: 발송 차감 (매출 인식 시점)
 * ADJUST: 운영자 강제 조정
 * EXPIRE: 5년 만료 소멸
 * REFUND: 환불
 * REVERT: 외부 적재 실패 보상
 */
public enum BalanceLedgerReason {
    SEND,
    ADJUST,
    EXPIRE,
    REFUND,
    REVERT
}
