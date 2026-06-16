package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.billing.BalanceLedgerReason;
import com.wisecan.unified.domain.billing.ChargeBalance;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.domain.billing.Refund;
import com.wisecan.unified.domain.billing.RefundStatus;
import com.wisecan.unified.dto.admin.AdminBillingDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.RefundRepository;
import com.wisecan.unified.service.billing.BalanceCacheEvictEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AdminBillingService 단위 테스트 — §12.5.
 */
@ExtendWith(MockitoExtension.class)
class AdminBillingServiceTest {

    @Mock private RefundRepository refundRepository;
    @Mock private ChargeBalanceRepository chargeBalanceRepository;
    @Mock private ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminBillingService adminBillingService;

    // ── 환불 목록 조회 ────────────────────────────────────────────────────

    @Test
    @DisplayName("전체 환불 목록 조회 — 회원 이메일 포함")
    void listAllRefunds_returnsAllWithEmail() {
        Refund refund = buildRefund(1L, 10000L, RefundStatus.PENDING);
        Member member = Member.builder()
                .email("user@example.com")
                .password("pw")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        given(refundRepository.findAllByOrderByRequestedAtDesc()).willReturn(List.of(refund));
        given(memberRepository.findAllById(any())).willReturn(List.of(member));

        List<AdminBillingDto.RefundSummary> result = adminBillingService.listAllRefunds();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualTo(10000L);
        assertThat(result.get(0).status()).isEqualTo(RefundStatus.PENDING);
    }

    // ── 캐시 강제 적립 ────────────────────────────────────────────────────

    @Test
    @DisplayName("캐시 강제 적립 — ChargeBalance INSERT + 원장 기록 + 캐시 무효화")
    void adjustCash_positive_creditAdded() {
        AdminBillingDto.CashAdjustRequest request =
                new AdminBillingDto.CashAdjustRequest(1L, 5000L, "이벤트 보상");

        given(memberRepository.existsById(1L)).willReturn(true);

        ChargeBalance saved = ChargeBalance.builder()
                .chargeId(0L)
                .memberId(1L)
                .methodType(PaymentMethodType.ADMIN_GRANT)
                .amountInitial(5000L)
                .expiresAt(LocalDateTime.now().plusYears(50))
                .build();
        given(chargeBalanceRepository.save(any())).willReturn(saved);
        given(chargeBalanceLedgerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminBillingDto.CashAdjustResponse response =
                adminBillingService.adjustCash(99L, request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.adjustedAmount()).isEqualTo(5000L);
        verify(chargeBalanceRepository).save(any(ChargeBalance.class));
        verify(eventPublisher).publishEvent(any(BalanceCacheEvictEvent.class));
    }

    // ── 캐시 강제 차감 ────────────────────────────────────────────────────

    @Test
    @DisplayName("캐시 강제 차감 — 잔액 충분 시 정상 처리")
    void adjustCash_negative_deductFromBalance() {
        AdminBillingDto.CashAdjustRequest request =
                new AdminBillingDto.CashAdjustRequest(1L, -3000L, "오류 보정 차감");

        given(memberRepository.existsById(1L)).willReturn(true);

        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L)
                .memberId(1L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(10000L)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .build();
        given(chargeBalanceRepository.findByMemberIdOrderByExpiresAtAscChargedAtAsc(1L))
                .willReturn(List.of(cb));
        given(chargeBalanceLedgerRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminBillingDto.CashAdjustResponse response =
                adminBillingService.adjustCash(99L, request);

        assertThat(response.adjustedAmount()).isEqualTo(-3000L);
        verify(eventPublisher).publishEvent(any(BalanceCacheEvictEvent.class));
    }

    @Test
    @DisplayName("캐시 강제 차감 — 잔액 부족 시 IllegalStateException")
    void adjustCash_negative_insufficientBalance_throwsException() {
        AdminBillingDto.CashAdjustRequest request =
                new AdminBillingDto.CashAdjustRequest(1L, -99999L, "차감 초과");

        given(memberRepository.existsById(1L)).willReturn(true);
        given(chargeBalanceRepository.findByMemberIdOrderByExpiresAtAscChargedAtAsc(1L))
                .willReturn(List.of());

        assertThatThrownBy(() -> adminBillingService.adjustCash(99L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    @DisplayName("조정 금액 0 — IllegalArgumentException")
    void adjustCash_zeroAmount_throwsException() {
        AdminBillingDto.CashAdjustRequest request =
                new AdminBillingDto.CashAdjustRequest(1L, 0L, "사유");

        assertThatThrownBy(() -> adminBillingService.adjustCash(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0이 될 수 없습니다");
    }

    @Test
    @DisplayName("존재하지 않는 회원 캐시 조정 — EntityNotFoundException")
    void adjustCash_memberNotFound_throwsException() {
        AdminBillingDto.CashAdjustRequest request =
                new AdminBillingDto.CashAdjustRequest(999L, 1000L, "사유");

        given(memberRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> adminBillingService.adjustCash(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private Refund buildRefund(Long memberId, Long amount, RefundStatus status) {
        return Refund.builder()
                .memberId(memberId)
                .chargeBalanceId(10L)
                .amount(amount)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .hasCashReceipt(false)
                .build();
    }
}
