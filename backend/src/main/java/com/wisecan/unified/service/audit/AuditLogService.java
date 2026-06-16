package com.wisecan.unified.service.audit;

import com.wisecan.unified.domain.audit.AuditLog;
import com.wisecan.unified.domain.audit.AuditLogEvent;
import com.wisecan.unified.dto.audit.AuditLogDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 내부 감사 서비스 (§12.9).
 * - 감사 로그 기록: 타 서비스에서 record()를 호출해 이벤트를 저장한다.
 * - 연간 감사 실행: executeAnnualAudit() — 해당 연도 이벤트 전수 집계 후 출력 이중 보관.
 * - 조회: SUPER_ADMIN 전용.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private static final String LOCAL_AUDIT_DIR = "audit-exports";

    private final AuditLogRepository auditLogRepository;

    /** 감사 이벤트 기록 — 타 서비스에서 호출 */
    @Transactional
    public void record(AuditLogEvent event, Long actorId, String actorEmail,
                       Long targetId, String targetType, String detail, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .event(event)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .targetId(targetId)
                .targetType(targetType)
                .detail(detail)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }

    /** 전체 감사 로그 페이징 조회 (SUPER_ADMIN 전용) */
    @Transactional(readOnly = true)
    public Page<AuditLogDto.Summary> listAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AuditLogDto.Summary::from);
    }

    /** 이벤트 유형별 필터 조회 */
    @Transactional(readOnly = true)
    public Page<AuditLogDto.Summary> listByEvent(AuditLogEvent event, Pageable pageable) {
        return auditLogRepository.findByEventOrderByCreatedAtDesc(event, pageable)
                .map(AuditLogDto.Summary::from);
    }

    /** 단건 상세 조회 */
    @Transactional(readOnly = true)
    public AuditLogDto.Detail detail(Long id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AuditLog", id));
        return AuditLogDto.Detail.from(auditLog);
    }

    /**
     * 연간 내부 감사 실행 (§12.9 — 매년 실행).
     * 1) 해당 연도 전체 감사 로그 조회
     * 2) 클라우드(DB) 보존은 이미 완료 상태
     * 3) 로컬 파일 내보내기(CSV) — 이중 보관
     * 4) 감사 실행 이벤트 자체도 기록
     */
    @Transactional
    public AuditLogDto.ExecuteResult executeAnnualAudit(int year, Long actorId, String actorEmail, String ipAddress) {
        LocalDateTime from = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime to = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<AuditLog> logs = auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(from, to);
        long totalEvents = logs.size();

        // 로컬 CSV 내보내기 (이중 보관)
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("audit_%d_%s.csv", year, timestamp);
        String exportPath = LOCAL_AUDIT_DIR + "/" + fileName;
        String backupPath = LOCAL_AUDIT_DIR + "/backup/" + fileName;

        try {
            writeCsv(logs, exportPath);
            writeCsv(logs, backupPath);
        } catch (IOException e) {
            log.error("[AUDIT] 로컬 파일 내보내기 실패: {}", e.getMessage());
        }

        // 감사 실행 자체 이벤트 기록
        record(AuditLogEvent.INTERNAL_AUDIT_EXECUTED,
                actorId, actorEmail, null, "ANNUAL_AUDIT",
                String.format("{\"year\":%d,\"totalEvents\":%d,\"exportPath\":\"%s\"}", year, totalEvents, exportPath),
                ipAddress);

        log.info("[AUDIT] {}년도 감사 실행 완료 — 총 {}건, 출력: {}", year, totalEvents, exportPath);

        return new AuditLogDto.ExecuteResult(year, totalEvents, exportPath, backupPath, LocalDateTime.now());
    }

    private void writeCsv(List<AuditLog> logs, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("id,event,actorId,actorEmail,targetId,targetType,ipAddress,createdAt\n");
        for (AuditLog l : logs) {
            sb.append(String.join(",",
                    str(l.getId()),
                    str(l.getEvent()),
                    str(l.getActorId()),
                    str(l.getActorEmail()),
                    str(l.getTargetId()),
                    str(l.getTargetType()),
                    str(l.getIpAddress()),
                    str(l.getCreatedAt())
            )).append("\n");
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private String str(Object val) {
        return val == null ? "" : val.toString().replace(",", ";");
    }
}
