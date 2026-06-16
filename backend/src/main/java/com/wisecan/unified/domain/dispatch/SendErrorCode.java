package com.wisecan.unified.domain.dispatch;

/**
 * 발송 정합성 검증 오류 코드 카탈로그.
 * 02_FEATURE_SPEC.md §6.1 예외 목록 및 §11 잔액 분기 처리 기반.
 */
public enum SendErrorCode {

    // ── 발신번호 ───────────────────────────────────────────────────
    CALLER_NOT_REGISTERED("발신번호가 등록되지 않았습니다."),
    CALLER_NOT_IN_WHITELIST("해당 발신번호는 이 API 키의 허용 목록에 없습니다."),

    // ── 스코프 ─────────────────────────────────────────────────────
    SCOPE_NOT_GRANTED("이 API 키에 발송 권한 스코프가 없습니다."),

    // ── 일일 한도 ──────────────────────────────────────────────────
    DAILY_LIMIT_EXCEEDED("일일 발송 한도를 초과했습니다."),
    TEST_QUOTA_EXCEEDED("테스트 키 채널별 발송 쿼터를 초과했습니다."),

    // ── 스팸 필터 ──────────────────────────────────────────────────
    SPAM_KEYWORD_DETECTED("스팸 키워드가 포함된 메시지는 발송할 수 없습니다."),

    // ── 광고 규정 ──────────────────────────────────────────────────
    AD_DISCLOSURE_MISSING("광고 메시지에는 '(광고)' 표기가 필요합니다."),
    AD_OPT_OUT_MISSING("광고 메시지에는 080 수신거부 번호 안내가 필요합니다."),
    AD_NIGHT_BLOCKED("야간(21시~08시)에는 광고성 메시지를 발송할 수 없습니다."),

    // ── 잔액 ───────────────────────────────────────────────────────
    INSUFFICIENT_BALANCE("잔액이 부족합니다."),

    // ── API 키 상태 ────────────────────────────────────────────────
    API_KEY_REVOKED("폐기된 API 키입니다."),
    API_KEY_NOT_ACTIVE("활성 상태가 아닌 API 키입니다."),

    // ── 망 분리 (W-205) ────────────────────────────────────────────
    TEST_KEY_PRODUCTION_ROUTE_DENIED("테스트 키로는 상용망 발송을 요청할 수 없습니다."),
    PRODUCTION_KEY_TEST_ROUTE_DENIED("상용 키로는 테스트망 발송을 요청할 수 없습니다."),

    // ── 카카오·RCS 템플릿 (W-305) ──────────────────────────────────
    TEMPLATE_REQUIRED("카카오/RCS 채널 발송 시 템플릿 코드는 필수입니다."),
    TEMPLATE_NOT_APPROVED("템플릿이 승인되지 않았거나 사용 불가 상태입니다."),

    // ── 보안·스팸·이상 패턴 자동 차단 (W-504) ─────────────────────
    /** 단시간 발송량 급증 — 슬라이딩 윈도우 임계 초과 */
    BURST_VOLUME_EXCEEDED("단시간 발송량 급증으로 발송이 차단되었습니다."),
    /** 동일 본문 패턴 반복 — 1분 내 동일 해시 n회 이상 */
    PATTERN_REPEAT_BLOCKED("동일 메시지 패턴 반복 발송이 감지되어 차단되었습니다."),
    /** 계정 이상 차단 상태 — AbuseDetectionService 가 SUSPENDED 처리 후 설정 */
    ACCOUNT_ABUSE_BLOCKED("보안 정책에 의해 발송이 차단된 계정입니다.");

    private final String defaultMessage;

    SendErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
