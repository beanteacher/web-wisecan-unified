package com.wisecan.unified.domain.billing;

/** 충전 거래 상태 — 02_FEATURE_SPEC §10.1 */
public enum ChargeStatus {
    REQUESTED,
    SUCCESS,
    FAILED,
    CANCELLED
}
