package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.AutoChargeConfig;
import com.wisecan.unified.domain.billing.PostpaidConfig;
import com.wisecan.unified.domain.billing.PostpaidBillingCycle;
import com.wisecan.unified.repository.billing.AutoChargeConfigRepository;
import com.wisecan.unified.repository.billing.PostpaidConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * W-405 잔액 부족 분기 단위 테스트 — 4분기 시나리오.
 *
 * 시나리오:
 *   분기1: 자동결제 활성 → AutoCharged
 *   분기2: 자동결제 비활성 + 후불 활성 → Postpaid
 *   분기3: 둘 다 비활성 + 부분 발송 선택 → Partial (HTTP 207)
 *   분기4: 둘 다 비활성 + 전체 취소 선택 → Cancelled (HTTP 402)
 */
@ExtendWith(MockitoExtension.class)
class InsufficientBalanceRouterTest {

    @Mock
    AutoChargeConfigRepository autoChargeConfigRepository;

    @Mock
    PostpaidConfigRepository postpaidConfigRepository;

    @Mock
    AutoChargeService autoChargeService;

    @Mock
    BalanceQueryService balanceQueryService;

    @InjectMocks
    InsufficientBalanceRouter router;

    private static final Long MEMBER_ID = 1L;
    private static final Long COMPANY_ID = 10L;
    private static final long UNIT_COST = 20L;

    // ── 분기 1: 자동결제 활성 ──────────────────────────────────────────

    @Test
    @DisplayName("분기1: 자동결제 활성 → 충전 후 AutoCharged 반환")
    void route_autoCharge_active_returnsAutoCharged() {
        // given
        long currentBalance = 100L;
        long totalCost = 500L;  // 잔액 부족
        List<String> recipients = List.of("01011111111", "01022222222");

        when(balanceQueryService.getBalanceFromDb(MEMBER_ID)).thenReturn(currentBalance, 600L); // 두 번 호출: 조회 후 충전 후
        AutoChargeConfig cfg = AutoChargeConfig.builder()
                .memberId(MEMBER_ID)
                .paymentMethodId(99L)
                .thresholdAmount(200L)
                .chargeAmount(1000L)
                .enabledYn("Y")
                .expiresAt(null)
                .build();
        when(autoChargeConfigRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(cfg));

        // when
        BalanceBranchResult result = router.route(
                MEMBER_ID, COMPANY_ID, totalCost, recipients, UNIT_COST, false);

        // then
        assertThat(result).isInstanceOf(BalanceBranchResult.AutoCharged.class);
        verify(autoChargeService).triggerIfNeeded(eq(MEMBER_ID), eq(currentBalance));
    }

    // ── 분기 2: 후불 활성 ─────────────────────────────────────────────

    @Test
    @DisplayName("분기2: 자동결제 비활성 + 후불 ACTIVE → Postpaid 반환")
    void route_postpaid_active_returnsPostpaid() {
        // given
        long currentBalance = 50L;
        long totalCost = 400L;
        List<String> recipients = List.of("01011111111", "01022222222");

        when(balanceQueryService.getBalanceFromDb(MEMBER_ID)).thenReturn(currentBalance);
        // 자동결제 설정 없음
        when(autoChargeConfigRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        PostpaidConfig postpaidCfg = PostpaidConfig.builder()
                .companyId(COMPANY_ID)
                .billingCycle(PostpaidBillingCycle.MONTHLY)
                .creditLimit(1_000_000L)
                .guaranteeInsuranceNo("INS-001")
                .build();
        postpaidCfg.startReview();
        postpaidCfg.approve(1_000_000L, "INS-001");
        when(postpaidConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(postpaidCfg));

        // when
        BalanceBranchResult result = router.route(
                MEMBER_ID, COMPANY_ID, totalCost, recipients, UNIT_COST, false);

        // then
        assertThat(result).isInstanceOf(BalanceBranchResult.Postpaid.class);
        BalanceBranchResult.Postpaid postpaid = (BalanceBranchResult.Postpaid) result;
        assertThat(postpaid.deferredAmount()).isEqualTo(totalCost);
        verify(autoChargeService, never()).triggerIfNeeded(anyLong(), anyLong());
    }

    // ── 분기 3: 부분 발송 선택 ────────────────────────────────────────

    @Test
    @DisplayName("분기3: 둘 다 비활성 + 부분 발송 선택 → Partial(HTTP 207) 반환")
    void route_bothInactive_partialChoice_returnsPartial() {
        // given
        long currentBalance = 60L;      // 20원 × 3건 = 60원 → 3건만 가능
        long totalCost = 100L;           // 5건 × 20원
        List<String> recipients = List.of(
                "01011111111", "01022222222", "01033333333", "01044444444", "01055555555");

        when(balanceQueryService.getBalanceFromDb(MEMBER_ID)).thenReturn(currentBalance);
        when(autoChargeConfigRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        when(postpaidConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        // when
        BalanceBranchResult result = router.route(
                MEMBER_ID, COMPANY_ID, totalCost, recipients, UNIT_COST, true);

        // then
        assertThat(result).isInstanceOf(BalanceBranchResult.Partial.class);
        BalanceBranchResult.Partial partial = (BalanceBranchResult.Partial) result;
        assertThat(partial.acceptedCount()).isEqualTo(3);
        assertThat(partial.rejectedCount()).isEqualTo(2);
        assertThat(partial.rejectedNumbers()).containsExactly("01044444444", "01055555555");
        assertThat(partial.shortfall()).isEqualTo(totalCost - currentBalance);
    }

    // ── 분기 4: 전체 취소 선택 ────────────────────────────────────────

    @Test
    @DisplayName("분기4: 둘 다 비활성 + 전체 취소 선택 → Cancelled 반환")
    void route_bothInactive_cancelChoice_returnsCancelled() {
        // given
        long currentBalance = 60L;
        long totalCost = 200L;
        List<String> recipients = List.of("01011111111", "01022222222", "01033333333");

        when(balanceQueryService.getBalanceFromDb(MEMBER_ID)).thenReturn(currentBalance);
        when(autoChargeConfigRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        when(postpaidConfigRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.empty());

        // when — partialChoice = false (전체 취소)
        BalanceBranchResult result = router.route(
                MEMBER_ID, COMPANY_ID, totalCost, recipients, UNIT_COST, false);

        // then
        assertThat(result).isInstanceOf(BalanceBranchResult.Cancelled.class);
        BalanceBranchResult.Cancelled cancelled = (BalanceBranchResult.Cancelled) result;
        assertThat(cancelled.shortfall()).isEqualTo(totalCost - currentBalance);
        verify(autoChargeService, never()).triggerIfNeeded(anyLong(), anyLong());
    }

    // ── 경계: 재조회 시 잔액 충분 ────────────────────────────────────

    @Test
    @DisplayName("재조회 시 이미 잔액 충분 → AutoCharged(0) 반환 (race-condition 방어)")
    void route_balanceSufficientOnRecheck_returnsAutoChargedZero() {
        // given — totalCost <= currentBalance (재조회 결과 충분)
        long currentBalance = 1000L;
        long totalCost = 800L;
        List<String> recipients = List.of("01011111111");

        when(balanceQueryService.getBalanceFromDb(MEMBER_ID)).thenReturn(currentBalance);

        // when
        BalanceBranchResult result = router.route(
                MEMBER_ID, COMPANY_ID, totalCost, recipients, UNIT_COST, false);

        // then
        assertThat(result).isInstanceOf(BalanceBranchResult.AutoCharged.class);
        BalanceBranchResult.AutoCharged autoCharged = (BalanceBranchResult.AutoCharged) result;
        assertThat(autoCharged.chargedAmount()).isEqualTo(0L);
        verify(autoChargeService, never()).triggerIfNeeded(anyLong(), anyLong());
        verify(autoChargeConfigRepository, never()).findByMemberId(anyLong());
    }
}
