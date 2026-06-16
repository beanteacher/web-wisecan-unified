package com.wisecan.unified.domain.template;

/**
 * SMS17 이관 처리 큐 상태.
 * 02_FEATURE_SPEC §9.1 "기존 SMS17 이관 시 운영자 처리 큐 진입" 참조.
 */
public enum TemplateTransferStatus {

    /** PENDING — 이관 신청 접수, 운영자 처리 대기 */
    PENDING,

    /** IN_PROGRESS — 운영자 처리 중 */
    IN_PROGRESS,

    /** COMPLETED — 이관 완료 */
    COMPLETED,

    /** REJECTED — 이관 거부 (사유 필수) */
    REJECTED,

    /** CANCELLED — 신청자 취소 */
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED;
    }
}
