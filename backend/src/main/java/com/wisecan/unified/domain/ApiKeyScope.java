package com.wisecan.unified.domain;

import java.util.Set;

/**
 * API Key 스코프 카탈로그 (12종).
 * 02_FEATURE_SPEC.md §5.3 및 §0 스코프 목록 기준.
 */
public enum ApiKeyScope {

    // 발송 계열
    SEND("send", "모든 채널 발송"),
    SEND_SMS("send:sms", "SMS/LMS/MMS 발송"),
    SEND_KAKAO("send:kakao", "카카오 알림톡 발송"),
    SEND_RCS("send:rcs", "RCS 발송"),

    // 조회 계열
    HISTORY_READ("history:read", "발송 이력 조회"),
    CALLBACK_READ("callback:read", "발신번호 조회"),
    CALLBACK_MANAGE("callback:manage", "발신번호 등록·삭제"),
    KEY_READ("key:read", "API 키 목록 조회"),
    BALANCE_READ("balance:read", "잔액 조회"),
    TEMPLATE_READ("template:read", "카카오·RCS 템플릿 조회"),
    BRAND_READ("brand:read", "RCS 브랜드 조회"),

    // 관리 계열
    WEBHOOK_MANAGE("webhook:manage", "수신 콜백 URL 설정");

    private final String value;
    private final String description;

    ApiKeyScope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /** 문자열 value 로부터 enum 역조회 */
    public static ApiKeyScope fromValue(String value) {
        for (ApiKeyScope scope : values()) {
            if (scope.value.equals(value)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("알 수 없는 스코프: " + value);
    }

    // ─── 권장 프리셋 ───────────────────────────────────────────

    /** 테스트 키 기본 프리셋: 발송 + 이력 조회만 */
    public static Set<ApiKeyScope> presetTest() {
        return Set.of(SEND, HISTORY_READ);
    }

    /** 발송 전용 프리셋: 모든 채널 발송 + 이력 조회 */
    public static Set<ApiKeyScope> presetSendOnly() {
        return Set.of(SEND, SEND_SMS, SEND_KAKAO, SEND_RCS, HISTORY_READ);
    }

    /** 읽기 전용 프리셋: 조회 계열 전체 */
    public static Set<ApiKeyScope> presetReadOnly() {
        return Set.of(HISTORY_READ, CALLBACK_READ, KEY_READ, BALANCE_READ, TEMPLATE_READ, BRAND_READ);
    }

    /** 전체 권한 프리셋 (운영 키 최대 범위) */
    public static Set<ApiKeyScope> presetFull() {
        return Set.of(values());
    }
}
