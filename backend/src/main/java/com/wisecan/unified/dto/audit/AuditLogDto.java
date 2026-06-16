package com.wisecan.unified.dto.audit;

import com.wisecan.unified.domain.audit.AuditLog;
import com.wisecan.unified.domain.audit.AuditLogEvent;

import java.time.LocalDateTime;

public class AuditLogDto {

    public record Summary(
            Long id,
            AuditLogEvent event,
            Long actorId,
            String actorEmail,
            Long targetId,
            String targetType,
            String ipAddress,
            LocalDateTime createdAt
    ) {
        public static Summary from(AuditLog log) {
            return new Summary(
                    log.getId(),
                    log.getEvent(),
                    log.getActorId(),
                    log.getActorEmail(),
                    log.getTargetId(),
                    log.getTargetType(),
                    log.getIpAddress(),
                    log.getCreatedAt()
            );
        }
    }

    public record Detail(
            Long id,
            AuditLogEvent event,
            Long actorId,
            String actorEmail,
            Long targetId,
            String targetType,
            String detail,
            String ipAddress,
            LocalDateTime createdAt
    ) {
        public static Detail from(AuditLog log) {
            return new Detail(
                    log.getId(),
                    log.getEvent(),
                    log.getActorId(),
                    log.getActorEmail(),
                    log.getTargetId(),
                    log.getTargetType(),
                    log.getDetail(),
                    log.getIpAddress(),
                    log.getCreatedAt()
            );
        }
    }

    /** 감사 실행 요청 (매년 실행 — §12.9) */
    public record ExecuteRequest(
            int year
    ) {}

    /** 감사 실행 결과 요약 */
    public record ExecuteResult(
            int year,
            long totalEvents,
            String exportPath,
            String backupPath,
            LocalDateTime executedAt
    ) {}
}
