package com.wisecan.unified.controller;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.dto.AdminStatsDto;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.AdminStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 운영자 대시보드·통계·시스템 설정 API — W-503
 *
 * <p>모든 엔드포인트는 ADMIN 또는 SUPER_ADMIN 역할이 필요하다.</p>
 *
 * <pre>
 * GET  /api/v1/admin/stats/dashboard          — 대시보드 요약 카드
 * GET  /api/v1/admin/stats/send?period=DAILY  — 발송량 통계 (DAILY|WEEKLY|MONTHLY)
 * GET  /api/v1/admin/stats/send/channels      — 채널별 발송 분포
 * GET  /api/v1/admin/stats/members            — 회원 증가 통계
 * GET  /api/v1/admin/settings                 — 시스템 설정 목록
 * GET  /api/v1/admin/settings/{key}           — 단일 설정 조회
 * PUT  /api/v1/admin/settings/{key}           — 설정 수정
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final MemberRepository memberRepository;

    // ── 대시보드 ──────────────────────────────────────────────────────

    /**
     * 운영자 대시보드 요약 카드 (캐시 5분 TTL).
     */
    @GetMapping("/stats/dashboard")
    public ResponseEntity<ApiResponse<AdminStatsDto.DashboardSummary>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDashboardSummary()));
    }

    // ── 발송량 통계 ──────────────────────────────────────────────────

    /**
     * 발송량 통계.
     *
     * @param period DAILY(최근 30일) | WEEKLY(최근 12주) | MONTHLY(최근 12개월). 기본 DAILY.
     */
    @GetMapping("/stats/send")
    public ResponseEntity<ApiResponse<AdminStatsDto.SendVolumeStats>> getSendStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "DAILY") String period) {
        resolveAdminId(userDetails);
        AdminStatsDto.SendVolumeStats stats = switch (period.toUpperCase()) {
            case "WEEKLY"  -> adminStatsService.getWeeklySendStats();
            case "MONTHLY" -> adminStatsService.getMonthlySendStats();
            default        -> adminStatsService.getDailySendStats();
        };
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 채널별 발송 분포 — 이번 달 기준.
     */
    @GetMapping("/stats/send/channels")
    public ResponseEntity<ApiResponse<List<AdminStatsDto.ChannelBreakdown>>> getChannelBreakdown(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getChannelBreakdown()));
    }

    // ── 회원 통계 ────────────────────────────────────────────────────

    /**
     * 회원 증가 통계.
     *
     * @param period DAILY(최근 30일) | WEEKLY | MONTHLY. 기본 DAILY.
     */
    @GetMapping("/stats/members")
    public ResponseEntity<ApiResponse<AdminStatsDto.MemberGrowthStats>> getMemberStats(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "DAILY") String period) {
        resolveAdminId(userDetails);
        // 현재 DAILY 지원; 확장 시 weekly/monthly 추가
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getDailyMemberStats()));
    }

    // ── 시스템 설정 ──────────────────────────────────────────────────

    /**
     * 시스템 설정 전체 목록.
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<AdminStatsDto.SystemSetting>>> listSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.listSystemSettings()));
    }

    /**
     * 단일 시스템 설정 조회.
     */
    @GetMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<AdminStatsDto.SystemSetting>> getSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String key) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(adminStatsService.getSystemSetting(key)));
    }

    /**
     * 시스템 설정 수정.
     */
    @PutMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<AdminStatsDto.SystemSetting>> updateSetting(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String key,
            @RequestBody @Valid AdminStatsDto.UpdateSettingRequest request) {
        Long adminId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminStatsService.updateSystemSetting(key, request.value(), adminId)));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────

    private Long resolveAdminId(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Member", 0L));
        MemberRole role = member.getRole();
        if (role != MemberRole.ADMIN && role != MemberRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("운영자 권한이 필요합니다.");
        }
        return member.getId();
    }
}
