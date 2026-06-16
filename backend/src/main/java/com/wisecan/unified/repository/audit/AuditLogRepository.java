package com.wisecan.unified.repository.audit;

import com.wisecan.unified.domain.audit.AuditLog;
import com.wisecan.unified.domain.audit.AuditLogEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEventOrderByCreatedAtDesc(AuditLogEvent event, Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtAsc(
            LocalDateTime from, LocalDateTime to);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
