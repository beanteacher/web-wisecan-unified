package com.wisecan.unified.service.audit;

import com.wisecan.unified.domain.audit.AuditLog;
import com.wisecan.unified.domain.audit.AuditLogEvent;
import com.wisecan.unified.dto.audit.AuditLogDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.audit.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    // ── record() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("감사 이벤트 기록 — 저장 호출 확인")
    void record_savesAuditLog() {
        auditLogService.record(
                AuditLogEvent.MEMBER_SUSPENDED,
                1L, "admin@wisecan.com",
                42L, "MEMBER",
                "{\"reason\":\"스팸\"}",
                "127.0.0.1"
        );

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // ── listAll() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 감사 로그 목록 조회 — 페이징 결과 반환")
    void listAll_returnsPagedResult() {
        AuditLog log = AuditLog.builder()
                .event(AuditLogEvent.CALLBACK_APPROVED)
                .actorId(1L)
                .actorEmail("admin@wisecan.com")
                .targetId(10L)
                .targetType("CALLBACK")
                .detail("{}")
                .ipAddress("127.0.0.1")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        given(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogDto.Summary> result = auditLogService.listAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).event()).isEqualTo(AuditLogEvent.CALLBACK_APPROVED);
        assertThat(result.getContent().get(0).targetType()).isEqualTo("CALLBACK");
    }

    // ── listByEvent() ────────────────────────────────────────────────────

    @Test
    @DisplayName("이벤트 유형별 필터 조회 — 해당 이벤트만 반환")
    void listByEvent_filtersCorrectly() {
        AuditLog log = AuditLog.builder()
                .event(AuditLogEvent.API_KEY_REVOKED)
                .actorId(2L)
                .actorEmail("admin@wisecan.com")
                .targetId(99L)
                .targetType("API_KEY")
                .detail("{}")
                .ipAddress("10.0.0.1")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        given(auditLogRepository.findByEventOrderByCreatedAtDesc(AuditLogEvent.API_KEY_REVOKED, pageable))
                .willReturn(new PageImpl<>(List.of(log)));

        Page<AuditLogDto.Summary> result = auditLogService.listByEvent(AuditLogEvent.API_KEY_REVOKED, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).event()).isEqualTo(AuditLogEvent.API_KEY_REVOKED);
    }

    // ── detail() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("단건 상세 조회 — 존재하는 로그 반환")
    void detail_returnsLog() {
        AuditLog log = AuditLog.builder()
                .event(AuditLogEvent.ADMIN_LOGIN)
                .actorId(1L)
                .actorEmail("admin@wisecan.com")
                .targetId(null)
                .targetType("AUTH")
                .detail("{\"success\":true}")
                .ipAddress("127.0.0.1")
                .build();

        given(auditLogRepository.findById(1L)).willReturn(Optional.of(log));

        AuditLogDto.Detail result = auditLogService.detail(1L);

        assertThat(result.event()).isEqualTo(AuditLogEvent.ADMIN_LOGIN);
        assertThat(result.actorEmail()).isEqualTo("admin@wisecan.com");
        assertThat(result.detail()).contains("success");
    }

    @Test
    @DisplayName("단건 상세 조회 — 존재하지 않으면 예외")
    void detail_notFound_throwsException() {
        given(auditLogRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.detail(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── executeAnnualAudit() ─────────────────────────────────────────────

    @Test
    @DisplayName("연간 감사 실행 — 이벤트 수 집계 및 감사 실행 이벤트 기록")
    void executeAnnualAudit_countsEventsAndRecords() {
        AuditLog log1 = AuditLog.builder()
                .event(AuditLogEvent.MEMBER_SUSPENDED)
                .actorId(1L).actorEmail("admin@wisecan.com")
                .targetId(10L).targetType("MEMBER").detail("{}").ipAddress("127.0.0.1")
                .build();
        AuditLog log2 = AuditLog.builder()
                .event(AuditLogEvent.CALLBACK_APPROVED)
                .actorId(1L).actorEmail("admin@wisecan.com")
                .targetId(20L).targetType("CALLBACK").detail("{}").ipAddress("127.0.0.1")
                .build();

        given(auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(log1, log2));

        AuditLogDto.ExecuteResult result =
                auditLogService.executeAnnualAudit(2025, 1L, "admin@wisecan.com", "127.0.0.1");

        assertThat(result.year()).isEqualTo(2025);
        assertThat(result.totalEvents()).isEqualTo(2);
        assertThat(result.exportPath()).contains("audit_2025");
        assertThat(result.backupPath()).contains("backup");
        // 감사 실행 이벤트 자체도 저장 (record() 내 save() 호출)
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
