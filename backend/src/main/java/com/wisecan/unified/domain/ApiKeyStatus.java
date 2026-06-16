package com.wisecan.unified.domain;

public enum ApiKeyStatus {
    /** 활성 상태 — 발급 직후(TEST) 또는 운영자 승인 후(PRODUCTION) */
    ACTIVE,
    /** 폐기 — 회원 직접 폐기 또는 운영자 반려 */
    REVOKED,
    /** 운영 키 검토 대기 — PRODUCTION 키 발급 후 운영자 승인 전 (§12.6) */
    PENDING_REVIEW
}
