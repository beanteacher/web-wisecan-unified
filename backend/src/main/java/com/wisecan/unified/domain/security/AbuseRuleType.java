package com.wisecan.unified.domain.security;

/**
 * 이상 패턴 탐지 규칙 유형.
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-004~007 참조.
 */
public enum AbuseRuleType {

    /** 단시간 발송량 급증 — 슬라이딩 윈도우 내 임계 초과 */
    BURST_VOLUME,

    /** 동일 메시지 본문 패턴 반복 — 1분 내 동일 해시 n회 이상 */
    PATTERN_REPEAT,

    /** 블랙리스트 키워드 탐지 (SpamKeywordGate 보완 레이어) */
    BLACKLIST_KEYWORD,

    /** 비정상 시간대 대량 발송 — 새벽 시간대 임계 이상 */
    ANOMALOUS_HOUR,

    /** 운영자 수동 차단 */
    MANUAL
}
