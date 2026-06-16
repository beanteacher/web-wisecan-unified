package com.wisecan.unified.domain.dispatch.encoding;

/**
 * SMS 메시지 타입.
 *
 * EUC-KR(CP949) 인코딩 기준 바이트 길이로 분류한다.
 * <ul>
 *   <li>SMS  — 90 byte 이하</li>
 *   <li>LMS  — 91 byte 이상 2,000 byte 이하</li>
 *   <li>MMS  — 멀티미디어 첨부가 있는 경우 (본문 길이 무관)</li>
 * </ul>
 *
 * 02_FEATURE_SPEC.md §6.1 발송 채널·타입 분기 기준.
 */
public enum SmsMessageType {

    /** 단문 문자 — EUC-KR 기준 최대 90 byte */
    SMS,

    /** 장문 문자 — EUC-KR 기준 91~2,000 byte */
    LMS,

    /**
     * 멀티미디어 문자 — 미디어 첨부 시 본문 길이와 관계없이 MMS.
     * 본문만 있는 경우에는 바이트 길이 기준(SMS/LMS)으로 결정한다.
     */
    MMS;

    /** SMS 바이트 상한 (EUC-KR 기준) */
    public static final int SMS_MAX_BYTES = 90;

    /** LMS/MMS 바이트 상한 (EUC-KR 기준) */
    public static final int LMS_MAX_BYTES = 2_000;
}
