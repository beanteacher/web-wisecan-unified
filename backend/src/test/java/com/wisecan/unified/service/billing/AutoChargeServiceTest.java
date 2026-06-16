package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.*;
import com.wisecan.unified.dto.billing.AutoChargeDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.billing.AutoChargeConfigRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.ChargeRepository;
import com.wisecan.unified.repository.billing.PaymentMethodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AutoChargeServiceTest {

    @Mock AutoChargeConfigRepository autoChargeConfigRepository;
    @Mock PaymentMethodRepository paymentMethodRepository;
    @Mock ChargeRepository chargeRepository;
    @Mock ChargeBalanceRepository chargeBalanceRepository;
    @Mock PgPaymentPort pgPaymentPort;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AutoChargeService autoChargeService;

    // ──────────────────────────────────────────
    // activate
    // ──────────────────────────────────────────

    @Test
    @DisplayName("빌링키 없는 결제수단으로 자동충전 활성화 — BillingException")
    void activate_noBillingKey_throwsBillingException() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                // pgBillingKey 미설정
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));

        AutoChargeDto.ActivateRequest request =
                new AutoChargeDto.ActivateRequest(1L, 5_000L, 10_000L, false);

        assertThatThrownBy(() -> autoChargeService.activate(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("빌링키가 등록된 결제수단만");
    }

    @Test
    @DisplayName("타인 결제수단으로 자동충전 활성화 — BillingException")
    void activate_anotherMemberPaymentMethod_throwsBillingException() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(999L) // 다른 회원
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 5678")
                .pgBillingKey("BK-OTHER")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));

        AutoChargeDto.ActivateRequest request =
                new AutoChargeDto.ActivateRequest(1L, 5_000L, 10_000L, false);

        assertThatThrownBy(() -> autoChargeService.activate(100L, request))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("본인의 결제수단이 아닙니다");
    }

    @Test
    @DisplayName("신규 자동충전 설정 저장 — 30일 제한 없이 무기한")
    void activate_newConfig_noExpiry_savesConfig() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-001")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.empty());

        AutoChargeConfig savedConfig = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.save(any(AutoChargeConfig.class))).willReturn(savedConfig);

        AutoChargeDto.ActivateRequest request =
                new AutoChargeDto.ActivateRequest(1L, 5_000L, 10_000L, false);
        AutoChargeDto.Response response = autoChargeService.activate(100L, request);

        assertThat(response.enabled()).isTrue();
        assertThat(response.expiresAt()).isNull();
        then(autoChargeConfigRepository).should().save(any(AutoChargeConfig.class));
    }

    @Test
    @DisplayName("신규 자동충전 설정 저장 — 30일 제한 적용")
    void activate_newConfig_with30DayLimit_setsExpiresAt() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-001")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.empty());

        LocalDate expectedExpiry = LocalDate.now().plusDays(30);
        AutoChargeConfig savedConfig = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(expectedExpiry)
                .build();
        given(autoChargeConfigRepository.save(any(AutoChargeConfig.class))).willReturn(savedConfig);

        AutoChargeDto.ActivateRequest request =
                new AutoChargeDto.ActivateRequest(1L, 5_000L, 10_000L, true);
        AutoChargeDto.Response response = autoChargeService.activate(100L, request);

        assertThat(response.expiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    @DisplayName("기존 설정 갱신 — update 호출 후 save")
    void activate_existingConfig_updatesAndSaves() {
        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-001")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));

        AutoChargeConfig existing = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(3_000L).chargeAmount(5_000L)
                .enabledYn("N").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(existing));
        given(autoChargeConfigRepository.save(any(AutoChargeConfig.class))).willReturn(existing);

        AutoChargeDto.ActivateRequest request =
                new AutoChargeDto.ActivateRequest(1L, 5_000L, 10_000L, false);
        autoChargeService.activate(100L, request);

        // 기존 객체가 update()로 수정되어 저장
        assertThat(existing.getThresholdAmount()).isEqualTo(5_000L);
        assertThat(existing.getChargeAmount()).isEqualTo(10_000L);
        assertThat(existing.isEnabled()).isTrue();
        then(autoChargeConfigRepository).should().save(existing);
    }

    // ──────────────────────────────────────────
    // getConfig
    // ──────────────────────────────────────────

    @Test
    @DisplayName("자동충전 설정 없는 회원 조회 — EntityNotFoundException")
    void getConfig_notFound_throwsEntityNotFoundException() {
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> autoChargeService.getConfig(100L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AutoChargeConfig");
    }

    // ──────────────────────────────────────────
    // deactivate
    // ──────────────────────────────────────────

    @Test
    @DisplayName("자동충전 해지 — enabledYn N 으로 변경")
    void deactivate_success_disablesConfig() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        autoChargeService.deactivate(100L);

        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("자동충전 해지 — 설정 없는 경우 EntityNotFoundException")
    void deactivate_notFound_throwsEntityNotFoundException() {
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> autoChargeService.deactivate(100L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ──────────────────────────────────────────
    // triggerIfNeeded
    // ──────────────────────────────────────────

    @Test
    @DisplayName("자동충전 설정 없으면 트리거 스킵")
    void triggerIfNeeded_noConfig_skips() {
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.empty());

        autoChargeService.triggerIfNeeded(100L, 1_000L);

        then(pgPaymentPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잔액이 임계치 초과 시 트리거 스킵")
    void triggerIfNeeded_balanceAboveThreshold_skips() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        autoChargeService.triggerIfNeeded(100L, 6_000L); // 임계치(5000) 초과

        then(pgPaymentPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("30일 만료된 설정 — 비활성화 후 트리거 스킵")
    void triggerIfNeeded_expiredConfig_disablesAndSkips() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(LocalDate.now().minusDays(1)) // 어제 만료
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        autoChargeService.triggerIfNeeded(100L, 1_000L);

        assertThat(config.isEnabled()).isFalse();
        then(pgPaymentPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("임계 도달 — PG 결제 성공 → CHARGE + CHARGE_BALANCE 저장, 캐시 이벤트")
    void triggerIfNeeded_thresholdReached_pgSuccess_savesChargeAndBalance() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-001")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(pgPaymentPort.requestPayment(anyLong(), any(), anyString(), anyLong(), anyString()))
                .willReturn(PgPaymentPort.PgPaymentResult.success("STUB-TX-AUTO-001"));

        Charge savedCharge = Charge.builder()
                .memberId(100L).paymentMethodId(1L).triggerId(config.getId())
                .chargeType(ChargeType.AUTO).amount(10_000L)
                .pgTxId("STUB-TX-AUTO-001").build();
        savedCharge.markSuccess();
        given(chargeRepository.save(any(Charge.class))).willReturn(savedCharge);

        ChargeBalance savedBalance = ChargeBalance.builder()
                .chargeId(1L).memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .amountInitial(10_000L)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .build();
        given(chargeBalanceRepository.save(any(ChargeBalance.class))).willReturn(savedBalance);

        autoChargeService.triggerIfNeeded(100L, 3_000L); // 임계치(5000) 이하

        then(chargeRepository).should().save(any(Charge.class));
        then(chargeBalanceRepository).should().save(any(ChargeBalance.class));
        then(eventPublisher).should().publishEvent(any(BalanceCacheEvictEvent.class));
        then(eventPublisher).should(never()).publishEvent(any(AutoChargeFailedEvent.class));
    }

    @Test
    @DisplayName("임계 도달 — PG 결제 실패 → CHARGE FAILED 저장, AutoChargeFailedEvent 발행")
    void triggerIfNeeded_thresholdReached_pgFailed_publishesFailedEvent() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                .pgBillingKey("BK-001")
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));
        given(pgPaymentPort.requestPayment(anyLong(), any(), anyString(), anyLong(), anyString()))
                .willReturn(PgPaymentPort.PgPaymentResult.failure("카드 한도 초과"));

        Charge failedCharge = Charge.builder()
                .memberId(100L).paymentMethodId(1L).triggerId(config.getId())
                .chargeType(ChargeType.AUTO).amount(10_000L)
                .pgTxId("FAILED-KEY").build();
        given(chargeRepository.save(any(Charge.class))).willReturn(failedCharge);

        autoChargeService.triggerIfNeeded(100L, 3_000L);

        then(chargeRepository).should().save(any(Charge.class));
        then(chargeBalanceRepository).shouldHaveNoInteractions();
        then(eventPublisher).should().publishEvent(any(AutoChargeFailedEvent.class));
        then(eventPublisher).should(never()).publishEvent(any(BalanceCacheEvictEvent.class));
    }

    @Test
    @DisplayName("임계 도달 — 빌링키 없는 결제수단이면 트리거 스킵")
    void triggerIfNeeded_noBillingKey_skips() {
        AutoChargeConfig config = AutoChargeConfig.builder()
                .memberId(100L).paymentMethodId(1L)
                .thresholdAmount(5_000L).chargeAmount(10_000L)
                .enabledYn("Y").expiresAt(null)
                .build();
        given(autoChargeConfigRepository.findByMemberId(100L)).willReturn(Optional.of(config));

        PaymentMethod pm = PaymentMethod.builder()
                .memberId(100L)
                .methodType(PaymentMethodType.CREDIT_CARD)
                .maskedLabel("**** 1234")
                // pgBillingKey 미설정
                .build();
        given(paymentMethodRepository.findById(1L)).willReturn(Optional.of(pm));

        autoChargeService.triggerIfNeeded(100L, 3_000L);

        then(pgPaymentPort).shouldHaveNoInteractions();
    }
}
