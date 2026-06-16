package com.wisecan.unified.domain.admin;

/**
 * 운영자 회원 통제 액션 열거형 — §12.3.
 *
 * MemberControlAuditLog.action 필드에 사용되며,
 * String 대신 enum 으로 관리해 오타·미정의 값 삽입을 컴파일 타임에 차단한다.
 */
public enum ControlAction {

    /** 강제 정지 — ACTIVE → SUSPENDED */
    SUSPEND,

    /** 강제 해지 — ACTIVE/SUSPENDED → TERMINATED */
    TERMINATE,

    /** 정지 해제 — SUSPENDED → ACTIVE */
    UNSUSPEND
}
