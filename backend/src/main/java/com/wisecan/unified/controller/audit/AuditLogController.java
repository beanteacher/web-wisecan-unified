package com.wisecan.unified.controller.audit;

import com.wisecan.unified.domain.audit.AuditLogEvent;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.audit.AuditLogDto;
import com.wisecan.unified.service.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 감사 로그 API (§12.9) — SUPER_ADMIN 전용.
 *
 * GET  /admin/audit              전체 감사 로그 목록 (페이징)
 * GET  /admin/audit/{id}         단건 상세
 * GET  /admin/audit?event=...    이벤트 유형 필터
 * POST /admin/audit/execute      연간 감사 실행 (매년 1회)
 */
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogDto.Summary>>> list(
            @RequestParam(required = false) AuditLogEvent event,
            Pageable pageable) {
        Page<AuditLogDto.Summary> result = event != null
                ? auditLogService.listByEvent(event, pageable)
                : auditLogService.listAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogDto.Detail>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.detail(id)));
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<AuditLogDto.ExecuteResult>> executeAnnualAudit(
            @RequestBody @Valid AuditLogDto.ExecuteRequest request,
            HttpServletRequest httpRequest) {
        // 실제 운영에서는 SecurityContext에서 actorId/actorEmail 추출
        // MVP 단계: 헤더 또는 고정값 사용
        Long actorId = null;
        String actorEmail = "admin@wisecan.com";
        String ipAddress = httpRequest.getRemoteAddr();

        AuditLogDto.ExecuteResult result =
                auditLogService.executeAnnualAudit(request.year(), actorId, actorEmail, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
