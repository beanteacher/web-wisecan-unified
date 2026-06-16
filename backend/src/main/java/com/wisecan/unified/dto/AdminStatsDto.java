package com.wisecan.unified.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 운영자 대시보드·통계 API DTO — W-503
 *
 * §12.3 대시보드 집계 / §12.4 통계 API
 */
public class AdminStatsDto {

    // ── 대시보드 요약 ────────────────────────────────────────────────

    /**
     * 오늘 기준 운영자 대시보드 요약 카드 데이터.
     * 캐시 TTL 5분.
     */
    public record DashboardSummary(
        long totalMembers,
        long newMembersToday,
        long totalSentToday,
        long totalSentThisWeek,
        long totalSentThisMonth,
        long revenueToday,
        long revenueThisWeek,
        long revenueThisMonth,
        long pendingSendRequests,
        long failedSendRequests
    ) {}

    // ── 발송량 통계 ──────────────────────────────────────────────────

    /**
     * 일/주/월별 발송량 집계 단위.
     */
    public record SendVolumePoint(
        LocalDate date,
        long count,
        long recipientCount,
        long totalCost
    ) {}

    /**
     * 발송량 통계 응답.
     */
    public record SendVolumeStats(
        String period,        // DAILY | WEEKLY | MONTHLY
        List<SendVolumePoint> data
    ) {}

    /**
     * 채널별 발송량 집계.
     */
    public record ChannelBreakdown(
        String channel,
        long count,
        long recipientCount,
        long totalCost
    ) {}

    // ── 회원 통계 ────────────────────────────────────────────────────

    /**
     * 일/주/월별 신규 회원 집계 단위.
     */
    public record MemberGrowthPoint(
        LocalDate date,
        long newMembers,
        long totalMembers
    ) {}

    /**
     * 회원 증가 통계 응답.
     */
    public record MemberGrowthStats(
        String period,
        List<MemberGrowthPoint> data
    ) {}

    // ── 매출 통계 ────────────────────────────────────────────────────

    /**
     * 일/주/월별 매출 집계 단위.
     */
    public record RevenuePoint(
        LocalDate date,
        long revenue,
        long chargeCount
    ) {}

    /**
     * 매출 통계 응답.
     */
    public record RevenueStats(
        String period,
        List<RevenuePoint> data
    ) {}

    // ── 시스템 설정 ──────────────────────────────────────────────────

    /**
     * 시스템 설정 단일 항목.
     */
    public record SystemSetting(
        String key,
        String value,
        String description,
        String updatedAt
    ) {}

    /**
     * 시스템 설정 수정 요청.
     */
    public record UpdateSettingRequest(
        @jakarta.validation.constraints.NotBlank(message = "설정값은 필수입니다") String value
    ) {}
}
