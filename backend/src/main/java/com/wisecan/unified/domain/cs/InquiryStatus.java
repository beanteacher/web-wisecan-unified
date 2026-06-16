package com.wisecan.unified.domain.cs;

public enum InquiryStatus {
    /** 접수 완료 — 아직 답변 없음 */
    OPEN,
    /** 운영자가 답변 작성 중 */
    IN_PROGRESS,
    /** 답변 완료 */
    ANSWERED,
    /** 문의자가 종료 처리 */
    CLOSED
}
