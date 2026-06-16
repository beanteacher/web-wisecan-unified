package com.wisecan.unified.domain.template;

/**
 * 카카오 알림톡 템플릿 상태.
 * 외부 시스템 kko_template.status 컬럼 값과 매핑된다.
 * 05_DATA_MODEL §6.1.2 참조.
 */
public enum KakaoTemplateStatus {

    /** A — 정상 */
    A,

    /** S — 중단 */
    S,

    /** N — 사용중지 */
    N,

    /** D — 삭제 */
    D;

    /** 발송 가능한 정상 상태인지 확인 */
    public boolean isActive() {
        return this == A;
    }
}
