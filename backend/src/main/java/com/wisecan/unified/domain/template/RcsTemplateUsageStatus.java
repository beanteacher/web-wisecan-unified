package com.wisecan.unified.domain.template;

/**
 * RCS 템플릿 사용 상태.
 * 외부 시스템 rcs_template.status 컬럼 값과 매핑된다.
 * 05_DATA_MODEL §6.2.1 참조.
 */
public enum RcsTemplateUsageStatus {

    /** ready — 사용 */
    READY("ready"),

    /** pause — 사용중지 */
    PAUSE("pause");

    private final String externalValue;

    RcsTemplateUsageStatus(String externalValue) {
        this.externalValue = externalValue;
    }

    public String getExternalValue() {
        return externalValue;
    }

    public static RcsTemplateUsageStatus fromExternal(String value) {
        if (value == null) return PAUSE;
        for (RcsTemplateUsageStatus s : values()) {
            if (s.externalValue.equals(value)) return s;
        }
        return PAUSE;
    }

    /** 발송 가능한 사용 상태인지 확인 */
    public boolean isReady() {
        return this == READY;
    }
}
