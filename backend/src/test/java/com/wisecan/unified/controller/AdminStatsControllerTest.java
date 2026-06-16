package com.wisecan.unified.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.dto.AdminStatsDto;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.AdminStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminStatsController 슬라이스 테스트 — W-503
 */
@WebMvcTest(AdminStatsController.class)
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminStatsService adminStatsService;

    @MockBean
    private MemberRepository memberRepository;

    // ── 공통 픽스처 ──────────────────────────────────────────────────

    private Member adminMember() {
        return Member.builder()
                .email("admin@wisecan.com")
                .password("encoded")
                .name("운영자")
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    // ── 대시보드 ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/stats/dashboard - 200 OK")
    void getDashboard_adminRole_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        AdminStatsDto.DashboardSummary summary = new AdminStatsDto.DashboardSummary(
                100L, 3L, 50L, 200L, 800L, 5000L, 20000L, 80000L, 5L, 2L);
        given(adminStatsService.getDashboardSummary()).willReturn(summary);

        mockMvc.perform(get("/api/v1/admin/stats/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMembers").value(100))
                .andExpect(jsonPath("$.data.totalSentToday").value(50))
                .andExpect(jsonPath("$.data.revenueToday").value(5000));
    }

    @Test
    @WithMockUser(username = "user@wisecan.com", roles = "USER")
    @DisplayName("GET /api/v1/admin/stats/dashboard - 일반 회원은 403")
    void getDashboard_normalUser_returns403() throws Exception {
        Member normalMember = Member.builder()
                .email("user@wisecan.com")
                .password("encoded")
                .name("일반회원")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();
        given(memberRepository.findByEmail("user@wisecan.com"))
                .willReturn(Optional.of(normalMember));

        mockMvc.perform(get("/api/v1/admin/stats/dashboard"))
                .andExpect(status().isForbidden());
    }

    // ── 발송량 통계 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/stats/send?period=DAILY - 200 OK")
    void getSendStats_daily_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        AdminStatsDto.SendVolumeStats stats = new AdminStatsDto.SendVolumeStats(
                "DAILY",
                List.of(new AdminStatsDto.SendVolumePoint(LocalDate.now(), 10L, 50L, 3000L))
        );
        given(adminStatsService.getDailySendStats()).willReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats/send").param("period", "DAILY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("DAILY"))
                .andExpect(jsonPath("$.data.data[0].count").value(10));
    }

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/stats/send?period=MONTHLY - 200 OK")
    void getSendStats_monthly_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        AdminStatsDto.SendVolumeStats stats = new AdminStatsDto.SendVolumeStats(
                "MONTHLY", List.of());
        given(adminStatsService.getMonthlySendStats()).willReturn(stats);

        mockMvc.perform(get("/api/v1/admin/stats/send").param("period", "MONTHLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("MONTHLY"));
    }

    // ── 채널별 분포 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/stats/send/channels - 200 OK")
    void getChannelBreakdown_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        List<AdminStatsDto.ChannelBreakdown> breakdown = List.of(
                new AdminStatsDto.ChannelBreakdown("SMS", 300L, 300L, 15000L),
                new AdminStatsDto.ChannelBreakdown("카카오 알림톡", 100L, 100L, 8000L)
        );
        given(adminStatsService.getChannelBreakdown()).willReturn(breakdown);

        mockMvc.perform(get("/api/v1/admin/stats/send/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].channel").value("SMS"))
                .andExpect(jsonPath("$.data[0].count").value(300));
    }

    // ── 시스템 설정 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("GET /api/v1/admin/settings - 200 OK")
    void listSettings_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        List<AdminStatsDto.SystemSetting> settings = List.of(
                new AdminStatsDto.SystemSetting("daily.send.limit", "10000", "일일 기본 발송 한도", "2026-06-16T00:00:00")
        );
        given(adminStatsService.listSystemSettings()).willReturn(settings);

        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].key").value("daily.send.limit"))
                .andExpect(jsonPath("$.data[0].value").value("10000"));
    }

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("PUT /api/v1/admin/settings/{key} - 200 OK")
    void updateSetting_returns200() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        AdminStatsDto.SystemSetting updated =
                new AdminStatsDto.SystemSetting("daily.send.limit", "20000", "일일 기본 발송 한도", "2026-06-16T12:00:00");
        given(adminStatsService.updateSystemSetting(eq("daily.send.limit"), eq("20000"), any()))
                .willReturn(updated);

        String body = objectMapper.writeValueAsString(new AdminStatsDto.UpdateSettingRequest("20000"));

        mockMvc.perform(put("/api/v1/admin/settings/daily.send.limit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("20000"));
    }

    @Test
    @WithMockUser(username = "admin@wisecan.com", roles = "ADMIN")
    @DisplayName("PUT /api/v1/admin/settings/{key} - 빈 값이면 400")
    void updateSetting_blankValue_returns400() throws Exception {
        given(memberRepository.findByEmail("admin@wisecan.com"))
                .willReturn(Optional.of(adminMember()));

        String body = objectMapper.writeValueAsString(new AdminStatsDto.UpdateSettingRequest(""));

        mockMvc.perform(put("/api/v1/admin/settings/daily.send.limit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
