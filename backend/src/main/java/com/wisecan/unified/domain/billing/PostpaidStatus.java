package com.wisecan.unified.domain.billing;

/**
 * 후불 설정 상태 — 05_DATA_MODEL §7.4.
 * APPLIED      : 신청 접수됨
 * UNDER_REVIEW : 운영자 심사 중
 * ACTIVE       : 심사 승인, 후불 발송 허용
 * SUSPENDED    : 정지 (연체 또는 운영자 처분)
 */
public enum PostpaidStatus {
    APPLIED,
    UNDER_REVIEW,
    ACTIVE,
    SUSPENDED
}
