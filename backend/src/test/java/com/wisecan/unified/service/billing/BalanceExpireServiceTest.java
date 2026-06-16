package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.BalanceLedgerReason;
import com.wisecan.unified.domain.billing.ChargeBalance;
import com.wisecan.unified.domain.billing.ChargeBalanceLedger;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceExpireServiceTest {

    @Mock ChargeBalanceRepository chargeBalanceRepository;
    @Mock ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks BalanceExpireService balanceExpireService;

    @Test
    @DisplayName("만료 대상 없음 — 처리 건수 0")
    void expireBalancesAsOf_noExpired_returnsZero() {
        given(chargeBalanceRepository.findExpired(any(LocalDateTime.class))).willReturn(List.of());

        int count = balanceExpireService.expireBalancesAsOf(LocalDateTime.now());

        assertThat(count).isZero();
        then(chargeBalanceLedgerRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("만료 잔액 소멸 — amountRemaining 0으로 변경 + EXPIRE 원장 기록 + 캐시 이벤트")
    void expireBalancesAsOf_hasExpired_processesAll() {
        ChargeBalance cb1 = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(30_000L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        ChargeBalance cb2 = ChargeBalance.builder()
                .chargeId(2L).memberId(200L)
                .methodType(PaymentMethodType.BANK_TRANSFER)
                .amountInitial(15_000L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        given(chargeBalanceRepository.findExpired(any(LocalDateTime.class))).willReturn(List.of(cb1, cb2));
        given(chargeBalanceLedgerRepository.save(any(ChargeBalanceLedger.class)))
                .willAnswer(inv -> inv.getArgument(0));

        int count = balanceExpireService.expireBalancesAsOf(LocalDateTime.now());

        assertThat(count).isEqualTo(2);
        assertThat(cb1.getAmountRemaining()).isZero();
        assertThat(cb2.getAmountRemaining()).isZero();
        then(chargeBalanceLedgerRepository).should(times(2)).save(any(ChargeBalanceLedger.class));
        then(eventPublisher).should(times(2)).publishEvent(any(BalanceCacheEvictEvent.class));
    }

    @Test
    @DisplayName("만료 소멸 원장 reason — EXPIRE 로 기록")
    void expireBalancesAsOf_ledgerReason_isExpire() {
        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.POINT)
                .amountInitial(10_000L)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        given(chargeBalanceRepository.findExpired(any(LocalDateTime.class))).willReturn(List.of(cb));
        given(chargeBalanceLedgerRepository.save(any(ChargeBalanceLedger.class)))
                .willAnswer(inv -> inv.getArgument(0));

        balanceExpireService.expireBalancesAsOf(LocalDateTime.now());

        then(chargeBalanceLedgerRepository).should().save(argThat(ledger ->
                BalanceLedgerReason.EXPIRE.equals(ledger.getReason())
                && ledger.getAmount() == -10_000L));
    }
}
