package com.wisecan.unified.domain.billing;

/**
 * 후불 청구서 상태 — 05_DATA_MODEL §7.4.
 * ISSUED   : 발행됨 (납부 대기)
 * PAID     : 결제 완료
 * OVERDUE  : 연체 — PostpaidBlockGate 가 이 상태를 조회해 발송 차단
 * CANCELLED: 취소 (운영자 처리)
 */
public enum PostpaidInvoiceStatus {
    ISSUED,
    PAID,
    OVERDUE,
    CANCELLED
}
