package com.wisecan.unified.service;

import com.wisecan.unified.domain.SystemSetting;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.AdminStatsDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.AdminMemberStatsRepository;
import com.wisecan.unified.repository.SystemSettingRepository;
import com.wisecan.unified.repository.dispatch.AdminSendStatsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AdminStatsService 단위 테스트 — W-503
 */
@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private AdminSendStatsRepository sendStatsRepository;

    @Mock
    private AdminMemberStatsRepository memberStatsRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private AdminStatsService adminStatsService;

    // ── 대시보드 요약 ────────────────────────────────────────────────

    @Test
    @DisplayName("대시보드 요약 - 정상 집계 반환")
    void getDashboardSummary_returnsAggregatedData() {
        // given
        given(memberStatsRepository.count()).willReturn(100L);
        given(memberStatsRepository.countByCreatedAtBetween(any(), any())).willReturn(3L);
        given(sendStatsRepository.countByCreatedAtBetween(any(), any()))
                .willReturn(50L)   // today
                .willReturn(200L)  // week
                .willReturn(800L); // month
        given(sendStatsRepository.sumTotalCostByCreatedAtBetween(any(), any()))
                .willReturn(5000L)
                .willReturn(20000L)
                .willReturn(80000L);
        given(sendStatsRepository.countByStatus(SendRequestStatus.PENDING)).willReturn(5L);
        given(sendStatsRepository.countByStatus(SendRequestStatus.FAILED)).willReturn(2L);

        // when
        AdminStatsDto.DashboardSummary summary = adminStatsService.getDashboardSummary();

        // then
        assertThat(summary.totalMembers()).isEqualTo(100L);
        assertThat(summary.newMembersToday()).isEqualTo(3L);
        assertThat(summary.totalSentToday()).isEqualTo(50L);
        assertThat(summary.totalSentThisWeek()).isEqualTo(200L);
        assertThat(summary.totalSentThisMonth()).isEqualTo(800L);
        assertThat(summary.revenueToday()).isEqualTo(5000L);
        assertThat(summary.pendingSendRequests()).isEqualTo(5L);
        assertThat(summary.failedSendRequests()).isEqualTo(2L);
    }

    // ── 발송량 통계 ──────────────────────────────────────────────────

    @Test
    @DisplayName("일별 발송량 통계 - DAILY 라벨과 데이터 반환")
    void getDailySendStats_returnsDailyLabeledStats() {
        // given
        LocalDate today = LocalDate.now();
        Object[] row = {today, 10L, 50L, 3000L};
        given(sendStatsRepository.aggregateDailyByCreatedAtBetween(any(), any()))
                .willReturn(List.of(row));

        // when
        AdminStatsDto.SendVolumeStats stats = adminStatsService.getDailySendStats();

        // then
        assertThat(stats.period()).isEqualTo("DAILY");
        assertThat(stats.data()).hasSize(1);
        assertThat(stats.data().get(0).count()).isEqualTo(10L);
        assertThat(stats.data().get(0).totalCost()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("월별 발송량 통계 - 같은 월 데이터가 합산됨")
    void getMonthlySendStats_aggregatesSameMonthData() {
        // given
        LocalDate day1 = LocalDate.of(2026, 5, 1);
        LocalDate day2 = LocalDate.of(2026, 5, 15);
        Object[] row1 = {day1, 10L, 50L, 2000L};
        Object[] row2 = {day2, 20L, 100L, 4000L};
        given(sendStatsRepository.aggregateDailyByCreatedAtBetween(any(), any()))
                .willReturn(List.of(row1, row2));

        // when
        AdminStatsDto.SendVolumeStats stats = adminStatsService.getMonthlySendStats();

        // then
        assertThat(stats.period()).isEqualTo("MONTHLY");
        assertThat(stats.data()).hasSize(1); // 같은 달이므로 1개로 합산
        assertThat(stats.data().get(0).count()).isEqualTo(30L);
        assertThat(stats.data().get(0).totalCost()).isEqualTo(6000L);
    }

    // ── 시스템 설정 ──────────────────────────────────────────────────

    @Test
    @DisplayName("시스템 설정 조회 - 존재하는 키 반환")
    void getSystemSetting_existingKey_returnsValue() {
        // given
        SystemSetting setting = SystemSetting.builder()
                .settingKey("daily.send.limit")
                .settingValue("10000")
                .description("일일 기본 발송 한도")
                .build();
        given(systemSettingRepository.findBySettingKey("daily.send.limit"))
                .willReturn(Optional.of(setting));

        // when
        AdminStatsDto.SystemSetting result = adminStatsService.getSystemSetting("daily.send.limit");

        // then
        assertThat(result.key()).isEqualTo("daily.send.limit");
        assertThat(result.value()).isEqualTo("10000");
        assertThat(result.description()).isEqualTo("일일 기본 발송 한도");
    }

    @Test
    @DisplayName("시스템 설정 조회 - 없는 키는 EntityNotFoundException")
    void getSystemSetting_missingKey_throwsException() {
        // given
        given(systemSettingRepository.findBySettingKey("nonexistent"))
                .willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminStatsService.getSystemSetting("nonexistent"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("시스템 설정 수정 - 값이 갱신됨")
    void updateSystemSetting_updatesValueAndReturns() {
        // given
        SystemSetting setting = SystemSetting.builder()
                .settingKey("daily.send.limit")
                .settingValue("10000")
                .description("일일 기본 발송 한도")
                .build();
        given(systemSettingRepository.findBySettingKey("daily.send.limit"))
                .willReturn(Optional.of(setting));

        // when
        AdminStatsDto.SystemSetting result =
                adminStatsService.updateSystemSetting("daily.send.limit", "20000", 1L);

        // then
        assertThat(result.value()).isEqualTo("20000");
        verify(systemSettingRepository).findBySettingKey("daily.send.limit");
    }

    @Test
    @DisplayName("시스템 설정 수정 - 없는 키는 EntityNotFoundException")
    void updateSystemSetting_missingKey_throwsException() {
        // given
        given(systemSettingRepository.findBySettingKey("bad.key"))
                .willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> adminStatsService.updateSystemSetting("bad.key", "value", 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── 주별 집계 헬퍼 검증 ──────────────────────────────────────────

    @Test
    @DisplayName("주별 발송량 통계 - 같은 주 데이터가 합산됨")
    void getWeeklySendStats_aggregatesSameWeekData() {
        // given — 같은 주(월~금)의 두 날
        LocalDate monday   = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate wednesday = monday.plusDays(2);
        Object[] row1 = {monday,    5L, 25L, 1000L};
        Object[] row2 = {wednesday, 7L, 35L, 1400L};
        given(sendStatsRepository.aggregateDailyByCreatedAtBetween(any(), any()))
                .willReturn(List.of(row1, row2));

        // when
        AdminStatsDto.SendVolumeStats stats = adminStatsService.getWeeklySendStats();

        // then
        assertThat(stats.period()).isEqualTo("WEEKLY");
        assertThat(stats.data()).hasSize(1);
        assertThat(stats.data().get(0).count()).isEqualTo(12L);
        assertThat(stats.data().get(0).totalCost()).isEqualTo(2400L);
    }
}
