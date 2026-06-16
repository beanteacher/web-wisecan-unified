package com.wisecan.unified.domain.template;

/**
 * RCS 템플릿 승인 상태.
 * 외부 시스템 rcs_template.approval_result 컬럼 값과 매핑된다.
 * 05_DATA_MODEL §6.2.1 참조.
 */
public enum RcsApprovalResult {

    /** 저장 */
    저장,

    /** 승인대기 */
    승인대기,

    /** 검수시작 */
    검수시작,

    /** 승인 */
    승인,

    /** 반려 */
    반려,

    /** 검수완료 */
    검수완료,

    /** 승인대기(수정) */
    승인대기_수정("승인대기(수정)"),

    /** 검수시작(수정) */
    검수시작_수정("검수시작(수정)"),

    /** 반려(수정) */
    반려_수정("반려(수정)"),

    /** 검수완료(수정) */
    검수완료_수정("검수완료(수정)");

    private final String externalValue;

    RcsApprovalResult() {
        this.externalValue = this.name();
    }

    RcsApprovalResult(String externalValue) {
        this.externalValue = externalValue;
    }

    public String getExternalValue() {
        return externalValue;
    }

    /** 외부 DB 문자열에서 enum으로 변환 */
    public static RcsApprovalResult fromExternal(String value) {
        if (value == null) return 저장;
        for (RcsApprovalResult r : values()) {
            if (r.externalValue.equals(value)) return r;
        }
        return 저장;
    }

    /** 발송 가능 승인 상태인지 확인 */
    public boolean isApproved() {
        return this == 승인;
    }
}
