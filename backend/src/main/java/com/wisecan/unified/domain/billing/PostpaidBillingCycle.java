package com.wisecan.unified.domain.billing;

/**
 * 후불 정산 주기 — 05_DATA_MODEL §7.4.
 * MONTHLY   : 월 1회 정산
 * BIWEEKLY  : 격주 정산
 */
public enum PostpaidBillingCycle {
    MONTHLY,
    BIWEEKLY
}
