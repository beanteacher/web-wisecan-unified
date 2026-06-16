package com.wisecan.unified.domain;

/**
 * API Key 유형 (02_FEATURE_SPEC.md §5.1, §5.2).
 * TEST: 채널별 발송 한도 있음, 실제 캐시 미차감.
 * PRODUCTION: 실사용 캐시 차감.
 */
public enum ApiKeyType {
    TEST,
    PRODUCTION
}
