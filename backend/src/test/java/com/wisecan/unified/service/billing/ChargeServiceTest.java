package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.Company;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.billing.*;
import com.wisecan.unified.dto.billing.ChargeDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.CompanyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.ChargeRepository;
import com.wisecan.unified.repository.billing.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChargeServiceTest {

    @Mock PaymentMethodRepository paymentMethodRepository;
    @Mock ChargeRepository chargeRepository;
    @Mock ChargeBalanceRepository chargeBalanceRepository;
    @Mock ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    @Mock PgPaymentPort pgPaymentPort;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock MemberRepository memberRepository;
    @Mock CompanyRepository companyRepository;

    @InjectMocks ChargeService chargeService;

    /** 기본: memberId 100L 은 개인회원(companyId 없음) → PREPAID 로 해석 */
    @BeforeEach
    void setUpPrepaidMember() {
        Member prepaidMember = mock(Member.class);
        lenient().when(prepaidMember.getCompanyId()).thenReturn(null);
        lenient().when(memberRepository.findById(100L)).thenReturn(Optional.of(prepaidMember));
    }

    @Test
    @DisplayName("POSTPAID 회원은 수동 충전 불가 — BillingException")
    void charge_postpaidMember_throwsBillingException() {
        Member companyMember = mock(Member.class);
        given(companyMember.getCompanyId()).willReturn(5L);
        given(memberRepository.findById(100L)).willReturn(Optional.of(companyMember));
        Company company = mock(Company.class);
        given(company.getBillingMode()).willReturn("POSTPAID");
        given(companyRepository.findById(5L)).willReturn(Optional.of(company));

        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        assertThatThrownBy(() -> chargeService.charge(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("후불 정산 회원");
    }

    @Test
    @DisplayName("존재하지 않는 결제수단 — EntityNotFoundException")
    void charge_paymentMethodNotFound_throwsEntityNotFoundException() {
        given(paymentMethodRepository.findById(99L)).willReturn(Optional.empty());
        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(99L, 10_000L);

        assertThatThrownBy(() -> chargeService.charge(100L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("타인의 결제수단 사용 시도 — BillingException")
    void charge_anotherMemberPaymentMethod_throwsBillingException() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(999L) // 다른 회원
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        assertThatThrownBy(() -> chargeService.charge(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("본인의 결제수단이 아닙니다");
    }

    @Test
    @DisplayName("비활성화된 결제수단 — BillingException")
    void charge_inactivePaymentMethod_throwsBillingException() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .build();
        pm.deactivate();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        assertThatThrownBy(() -> chargeService.charge(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("비활성화된 결제수단");
    }

    @Test
    @DisplayName("PG 결제 실패 — BillingException, CHARGE 행 FAILED 상태")
    void charge_pgFailed_throwsBillingException() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(pgPaymentPort.requestPayment(anyLong(), any(), any(), anyLong(), anyString()))
                .willReturn(PgPaymentPort.PgPaymentResult.failure("카드 한도 초과"));

        Charge failedCharge = Charge.builder()
                .memberId(100L).paymentMethodId(1L)
                .chargeType(ChargeType.MANUAL).amount(10_000L)
                .pgTxId("FAILED-KEY").build();
        given(chargeRepository.save(any(Charge.class))).willReturn(failedCharge);

        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        assertThatThrownBy(() -> chargeService.charge(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("결제 실패");

        then(chargeBalanceRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("충전 성공 — CHARGE + CHARGE_BALANCE 저장, 캐시 이벤트 발행")
    void charge_success_savesChargeAndBalance() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(pgPaymentPort.requestPayment(anyLong(), any(), any(), anyLong(), anyString()))
                .willReturn(PgPaymentPort.PgPaymentResult.success("STUB-TX-001"));

        Charge savedCharge = Charge.builder()
                .memberId(100L).paymentMethodId(1L)
                .chargeType(ChargeType.MANUAL).amount(10_000L)
                .pgTxId("STUB-TX-001").build();
        savedCharge.markSuccess();
        given(chargeRepository.save(any(Charge.class))).willReturn(savedCharge);

        ChargeBalance savedBalance = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(10_000L)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .build();
        given(chargeBalanceRepository.save(any(ChargeBalance.class))).willReturn(savedBalance);

        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);
        ChargeDto.Response response = chargeService.charge(100L, request);

        assertThat(response.status()).isEqualTo(ChargeStatus.SUCCESS);
        assertThat(response.pgTxId()).isEqualTo("STUB-TX-001");
        then(chargeBalanceRepository).should().save(any(ChargeBalance.class));
        then(eventPublisher).should().publishEvent(any(BalanceCacheEvictEvent.class));
    }

    @Test
    @DisplayName("발송 차감 — FIFO 잔액 차감 후 원장 기록")
    void deductForSend_success_recordsLedger() {
        given(chargeBalanceLedgerRepository.findByExternalSendIdAndReason("SEND-001", BalanceLedgerReason.SEND))
                .willReturn(Optional.empty());

        ChargeBalance cb = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(50_000L)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .build();
        given(chargeBalanceRepository.findDeductibleByMemberId(eq(100L), any(LocalDateTime.class)))
                .willReturn(List.of(cb));
        given(chargeBalanceLedgerRepository.save(any(ChargeBalanceLedger.class)))
                .willAnswer(inv -> inv.getArgument(0));

        long deducted = chargeService.deductForSend(100L, 10_000L, "SEND-001");

        assertThat(deducted).isEqualTo(10_000L);
        assertThat(cb.getAmountRemaining()).isEqualTo(40_000L);
        then(chargeBalanceLedgerRepository).should().save(any(ChargeBalanceLedger.class));
        then(eventPublisher).should().publishEvent(any(BalanceCacheEvictEvent.class));
    }

    @Test
    @DisplayName("발송 차감 — 잔액 부족 시 BillingException")
    void deductForSend_insufficientBalance_throwsBillingException() {
        given(chargeBalanceLedgerRepository.findByExternalSendIdAndReason(anyString(), any()))
                .willReturn(Optional.empty());
        given(chargeBalanceRepository.findDeductibleByMemberId(anyLong(), any(LocalDateTime.class)))
                .willReturn(List.of());

        assertThatThrownBy(() -> chargeService.deductForSend(100L, 5_000L, "SEND-002"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    @DisplayName("발송 차감 멱등성 — 동일 externalSendId 중복 차감 방지")
    void deductForSend_duplicate_skipsDeduction() {
        ChargeBalanceLedger existing = ChargeBalanceLedger.builder()
                .chargeBalanceId(1L).amount(-5_000L)
                .reason(BalanceLedgerReason.SEND).externalSendId("SEND-003").build();
        given(chargeBalanceLedgerRepository.findByExternalSendIdAndReason("SEND-003", BalanceLedgerReason.SEND))
                .willReturn(Optional.of(existing));

        long result = chargeService.deductForSend(100L, 5_000L, "SEND-003");

        assertThat(result).isEqualTo(5_000L);
        then(chargeBalanceRepository).shouldHaveNoInteractions();
    }
}
