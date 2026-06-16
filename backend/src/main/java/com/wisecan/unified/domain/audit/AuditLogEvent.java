package com.wisecan.unified.domain.audit;

/**
 * 감사 로그 이벤트 유형.
 * SUPER_ADMIN 권한으로 조회 가능 (§12.9).
 */
public enum AuditLogEvent {

    // 회원 관련
    MEMBER_SUSPENDED,
    MEMBER_TERMINATED,
    MEMBER_ROLE_CHANGED,

    // 발신번호 관련
    CALLBACK_APPROVED,
    CALLBACK_REJECTED,
    CALLBACK_DELETED,

    // API 키 관련
    API_KEY_ACTIVATED,
    API_KEY_REVOKED,

    // 내부 감사
    INTERNAL_AUDIT_EXECUTED,

    // OCR
    OCR_DOCUMENT_EXTRACTED,

    // 운영자 계정
    ADMIN_LOGIN,
    ADMIN_ROLE_GRANTED,
    ADMIN_ROLE_REVOKED
}
