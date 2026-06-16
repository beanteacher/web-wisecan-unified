package com.wisecan.unified.domain.audit;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 내부 감사 로그 — SUPER_ADMIN 전용 조회(§12.9).
 * 매년 1회 감사 실행 결과 + 운영자 액션 이력을 단일 테이블에 기록한다.
 * 출력은 클라우드(DB 보존) + 로컬 파일 이중 보관(AuditExportService).
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_log_event", columnList = "event"),
    @Index(name = "idx_audit_log_actor", columnList = "actor_id"),
    @Index(name = "idx_audit_log_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이벤트 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditLogEvent event;

    /** 행위자(운영자) 회원 ID */
    @Column(name = "actor_id")
    private Long actorId;

    /** 행위자 이메일 — 계정 삭제 후에도 추적 가능하도록 비정규화 저장 */
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    /** 대상 식별자 (회원 ID, 발신번호 ID, 키 ID 등) */
    @Column(name = "target_id")
    private Long targetId;

    /** 대상 유형 설명 (예: "MEMBER", "CALLBACK", "API_KEY") */
    @Column(name = "target_type", length = 50)
    private String targetType;

    /** 변경 내역 요약 (JSON 직렬화 문자열) */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** 요청 IP */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AuditLog(AuditLogEvent event, Long actorId, String actorEmail,
                    Long targetId, String targetType, String detail, String ipAddress) {
        this.event = event;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.targetId = targetId;
        this.targetType = targetType;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }
}
