package com.wisecan.unified.domain.template;

/**
 * 카카오 알림톡 템플릿 검수 상태.
 * 외부 시스템 kko_template.inspection_status 컬럼 값과 매핑된다.
 * 05_DATA_MODEL §6.1.2 참조.
 */
public enum KakaoInspectionStatus {

    /** REG — 등록 (검수 미요청) */
    REG,

    /** REQ — 검수 요청 */
    REQ,

    /** APR — 승인 */
    APR,

    /** REJ — 반려 */
    REJ;

    /** 발송 가능 상태인지 확인 */
    public boolean isApproved() {
        return this == APR;
    }
}
