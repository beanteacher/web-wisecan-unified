package com.wisecan.unified.domain.admin;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 통제 감사 로그 — §12.3.
 *
 * 운영자가 회원에게 가한 정지·해지·해제 사건을 불변 기록으로 보존한다.
 * 삭제·수정 불가 설계 (updatable=false 전 컬럼).
 *
 * M-1 리뷰 반영: action 필드를 String → ControlAction enum 으로 변경해
 * 오타·미정의 값 삽입을 컴파일 타임에 차단한다.
 */
@Entity
@Table(
    name = "member_control_audit_log",
    indexes = {
        @Index(name = "idx_mca_member", columnList = "member_id"),
        @Index(name = "idx_mca_operator", columnList = "operator_id"),
        @Index(name = "idx_mca_occurred", columnList = "occurred_at")
    }
)
@Getter
@NoArgsConstructor
public class MemberControlAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    /** SUSPEND / TERMINATE / UNSUSPEND */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private ControlAction action;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "operator_id", nullable = false, updatable = false)
    private Long operatorId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        this.occurredAt = LocalDateTime.now();
    }

    @Builder
    public MemberControlAuditLog(Long memberId, ControlAction action, String reason, Long operatorId) {
        this.memberId = memberId;
        this.action = action;
        this.reason = reason;
        this.operatorId = operatorId;
    }
}
