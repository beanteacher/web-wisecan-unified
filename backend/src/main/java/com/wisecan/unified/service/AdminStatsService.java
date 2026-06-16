package com.wisecan.unified.service;

import com.wisecan.unified.domain.SystemSetting;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.AdminStatsDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.AdminMemberStatsRepository;
import com.wisecan.unified.repository.SystemSettingRepository;
import com.wisecan.unified.repository.dispatch.AdminSendStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 운영자 대시보드·통계·시스템 설정 서비스 — W-503
 *
 * <p>DoD: 일/주/월 통계 + 데이터 갱신 5분 이하.
 * 캐시 TTL = 5분 (application.yml spring.cache.redis.time-to-live 설정과 별개로
 * 각 메서드에 @Cacheable 을 적용해 300초 TTL 을 지정한다).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatsService {

    private final AdminSendStatsRepository sendStatsRepository;
    private final AdminMemberStatsRepository memberStatsRepository;
    private final SystemSettingRepository systemSettingRepository;

    private static final String CACHE_DASHBOARD   = "admin:dashboard";
    private static final String CACHE_SEND_DAILY  = "admin:stats:send:daily";
    private static final String CACHE_SEND_WEEKLY = "admin:stats:send:weekly";
    private static final String CACHE_SEND_MONTHLY= "admin:stats:send:monthly";
    private static final String CACHE_MEMBER_DAILY= "admin:stats:member:daily";
    private static final String CACHE_CHANNEL     = "admin:stats:channel";
    private static final String CACHE_SETTINGS    = "admin:settings";

    // ── 대시보드 요약 ────────────────────────────────────────────────

    /**
     * 운영자 대시보드 요약 카드 (TTL 5분).
     */
    @Cacheable(value = CACHE_DASHBOARD, key = "'summary'")
    @Transactional(readOnly = true)
    public AdminStatsDto.DashboardSummary getDashboardSummary() {
        LocalDateTime todayStart  = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd    = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart   = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime monthStart  = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long totalMembers     = memberStatsRepository.count();
        long newMembersToday  = memberStatsRepository.countByCreatedAtBetween(todayStart, todayEnd);

        long sentToday        = sendStatsRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long sentThisWeek     = sendStatsRepository.countByCreatedAtBetween(weekStart, todayEnd);
        long sentThisMonth    = sendStatsRepository.countByCreatedAtBetween(monthStart, todayEnd);

        long revenueToday     = sendStatsRepository.sumTotalCostByCreatedAtBetween(todayStart, todayEnd);
        long revenueThisWeek  = sendStatsRepository.sumTotalCostByCreatedAtBetween(weekStart, todayEnd);
        long revenueThisMonth = sendStatsRepository.sumTotalCostByCreatedAtBetween(monthStart, todayEnd);

        long pending  = sendStatsRepository.countByStatus(SendRequestStatus.PENDING);
        long failed   = sendStatsRepository.countByStatus(SendRequestStatus.FAILED);

        log.debug("대시보드 요약 집계: members={}, sentToday={}, revenueToday={}",
                totalMembers, sentToday, revenueToday);

        return new AdminStatsDto.DashboardSummary(
                totalMembers,
                newMembersToday,
                sentToday,
                sentThisWeek,
                sentThisMonth,
                revenueToday,
                revenueThisWeek,
                revenueThisMonth,
                pending,
                failed
        );
    }

    // ── 발송량 통계 ──────────────────────────────────────────────────

    /**
     * 일별 발송량 통계 — 최근 30일 (TTL 5분).
     */
    @Cacheable(value = CACHE_SEND_DAILY, key = "'last30'")
    @Transactional(readOnly = true)
    public AdminStatsDto.SendVolumeStats getDailySendStats() {
        LocalDateTime from = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = sendStatsRepository.aggregateDailyByCreatedAtBetween(from, to);
        List<AdminStatsDto.SendVolumePoint> points = rows.stream()
                .map(row -> new AdminStatsDto.SendVolumePoint(
                        toLocalDate(row[0]),
                        toLong(row[1]),
                        toLong(row[2]),
                        toLong(row[3])
                ))
                .collect(Collectors.toList());

        return new AdminStatsDto.SendVolumeStats("DAILY", points);
    }

    /**
     * 주별 발송량 통계 — 최근 12주 (TTL 5분).
     */
    @Cacheable(value = CACHE_SEND_WEEKLY, key = "'last12w'")
    @Transactional(readOnly = true)
    public AdminStatsDto.SendVolumeStats getWeeklySendStats() {
        LocalDateTime from = LocalDate.now().minusWeeks(11).with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = sendStatsRepository.aggregateDailyByCreatedAtBetween(from, to);
        List<AdminStatsDto.SendVolumePoint> weekly = aggregateWeekly(rows);

        return new AdminStatsDto.SendVolumeStats("WEEKLY", weekly);
    }

    /**
     * 월별 발송량 통계 — 최근 12개월 (TTL 5분).
     */
    @Cacheable(value = CACHE_SEND_MONTHLY, key = "'last12m'")
    @Transactional(readOnly = true)
    public AdminStatsDto.SendVolumeStats getMonthlySendStats() {
        LocalDateTime from = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = sendStatsRepository.aggregateDailyByCreatedAtBetween(from, to);
        List<AdminStatsDto.SendVolumePoint> monthly = aggregateMonthly(rows);

        return new AdminStatsDto.SendVolumeStats("MONTHLY", monthly);
    }

    /**
     * 채널별 발송 분포 — 이번 달 기준 (TTL 5분).
     */
    @Cacheable(value = CACHE_CHANNEL, key = "'thisMonth'")
    @Transactional(readOnly = true)
    public List<AdminStatsDto.ChannelBreakdown> getChannelBreakdown() {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = sendStatsRepository.aggregateByChannelBetween(from, to);
        return rows.stream()
                .map(row -> new AdminStatsDto.ChannelBreakdown(
                        row[0] instanceof SendChannel ch ? ch.getDisplayName() : String.valueOf(row[0]),
                        toLong(row[1]),
                        toLong(row[2]),
                        toLong(row[3])
                ))
                .collect(Collectors.toList());
    }

    // ── 회원 통계 ────────────────────────────────────────────────────

    /**
     * 일별 신규 회원 통계 — 최근 30일 (TTL 5분).
     */
    @Cacheable(value = CACHE_MEMBER_DAILY, key = "'last30'")
    @Transactional(readOnly = true)
    public AdminStatsDto.MemberGrowthStats getDailyMemberStats() {
        LocalDateTime from = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(LocalTime.MAX);

        List<Object[]> rows = memberStatsRepository.aggregateDailyNewMembersBetween(from, to);

        List<AdminStatsDto.MemberGrowthPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date     = toLocalDate(row[0]);
            long newMembers    = toLong(row[1]);
            long total         = memberStatsRepository.countByCreatedAtBefore(date.plusDays(1).atStartOfDay());
            points.add(new AdminStatsDto.MemberGrowthPoint(date, newMembers, total));
        }

        return new AdminStatsDto.MemberGrowthStats("DAILY", points);
    }

    // ── 시스템 설정 ──────────────────────────────────────────────────

    /**
     * 전체 시스템 설정 목록 조회 (TTL 5분).
     */
    @Cacheable(value = CACHE_SETTINGS, key = "'all'")
    @Transactional(readOnly = true)
    public List<AdminStatsDto.SystemSetting> listSystemSettings() {
        return systemSettingRepository.findAll().stream()
                .map(s -> new AdminStatsDto.SystemSetting(
                        s.getSettingKey(),
                        s.getSettingValue(),
                        s.getDescription(),
                        s.getUpdatedAt() != null ? s.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
                ))
                .collect(Collectors.toList());
    }

    /**
     * 단일 시스템 설정 조회.
     */
    @Transactional(readOnly = true)
    public AdminStatsDto.SystemSetting getSystemSetting(String key) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SystemSetting", 0L));
        return new AdminStatsDto.SystemSetting(
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getUpdatedAt() != null ? setting.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null
        );
    }

    /**
     * 시스템 설정 수정 — 캐시 무효화 포함.
     *
     * @param key     설정 키
     * @param value   새 설정값
     * @param adminId 변경 운영자 ID
     */
    @CacheEvict(value = CACHE_SETTINGS, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public AdminStatsDto.SystemSetting updateSystemSetting(String key, String value, Long adminId) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElseThrow(() -> new EntityNotFoundException("SystemSetting", 0L));

        setting.updateValue(value, adminId);
        log.info("시스템 설정 변경: key={}, adminId={}", key, adminId);

        return new AdminStatsDto.SystemSetting(
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    // ── 캐시 수동 무효화 ─────────────────────────────────────────────

    /**
     * 대시보드·통계 캐시 전체 무효화 (배치·이벤트 등에서 호출).
     */
    @CacheEvict(value = {CACHE_DASHBOARD, CACHE_SEND_DAILY, CACHE_SEND_WEEKLY,
                          CACHE_SEND_MONTHLY, CACHE_MEMBER_DAILY, CACHE_CHANNEL}, allEntries = true)
    public void evictStatsCache() {
        log.info("운영자 통계 캐시 전체 무효화");
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────

    private LocalDate toLocalDate(Object obj) {
        if (obj instanceof LocalDate d) return d;
        if (obj instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(String.valueOf(obj));
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(obj));
    }

    /**
     * 일별 데이터를 주별로 집계 (월요일 기준).
     */
    private List<AdminStatsDto.SendVolumePoint> aggregateWeekly(List<Object[]> rows) {
        Map<LocalDate, AdminStatsDto.SendVolumePoint> weekMap = new java.util.TreeMap<>();
        for (Object[] row : rows) {
            LocalDate date     = toLocalDate(row[0]);
            LocalDate weekKey  = date.with(java.time.DayOfWeek.MONDAY);
            AdminStatsDto.SendVolumePoint existing = weekMap.get(weekKey);
            long count         = toLong(row[1]);
            long recipients    = toLong(row[2]);
            long cost          = toLong(row[3]);
            if (existing == null) {
                weekMap.put(weekKey, new AdminStatsDto.SendVolumePoint(weekKey, count, recipients, cost));
            } else {
                weekMap.put(weekKey, new AdminStatsDto.SendVolumePoint(
                        weekKey,
                        existing.count() + count,
                        existing.recipientCount() + recipients,
                        existing.totalCost() + cost
                ));
            }
        }
        return new ArrayList<>(weekMap.values());
    }

    /**
     * 일별 데이터를 월별로 집계 (월 첫날 기준).
     */
    private List<AdminStatsDto.SendVolumePoint> aggregateMonthly(List<Object[]> rows) {
        Map<LocalDate, AdminStatsDto.SendVolumePoint> monthMap = new java.util.TreeMap<>();
        for (Object[] row : rows) {
            LocalDate date     = toLocalDate(row[0]);
            LocalDate monthKey = date.withDayOfMonth(1);
            AdminStatsDto.SendVolumePoint existing = monthMap.get(monthKey);
            long count         = toLong(row[1]);
            long recipients    = toLong(row[2]);
            long cost          = toLong(row[3]);
            if (existing == null) {
                monthMap.put(monthKey, new AdminStatsDto.SendVolumePoint(monthKey, count, recipients, cost));
            } else {
                monthMap.put(monthKey, new AdminStatsDto.SendVolumePoint(
                        monthKey,
                        existing.count() + count,
                        existing.recipientCount() + recipients,
                        existing.totalCost() + cost
                ));
            }
        }
        return new ArrayList<>(monthMap.values());
    }
}
