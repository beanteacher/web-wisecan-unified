package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.*;
import com.wisecan.unified.dto.billing.AutoChargeDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.billing.AutoChargeConfigRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.ChargeRepository;
import com.wisecan.unified.repository.billing.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 자동충전 서비스 — W-402.
 *
 * 기능:
 *   1. 자동충전 설정 활성화·갱신 (POST /billing/auto-charge)
 *   2. 자동충전 설정 조회 (GET /billing/auto-charge)
 *   3. 자동충전 해지 (DELETE /billing/auto-charge)
 *   4. 임계 도달 트리거 — 잔액이 임계치 이하일 때 PG 빌링키 기반 정기결제 실행
 *      실패 시 AutoChargeFailedEvent 발행 (AFTER_COMMIT 알림)
 *
 * 30일 제한: ActivateRequest.apply30DayLimit = true 이면 expiresAt = 오늘 + 30일.
 * 멱등성: Charge.pgTxId UNIQUE 제약이 중복 결제를 차단.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoChargeService {

    private final AutoChargeConfigRepository autoChargeConfigRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeBalanceRepository chargeBalanceRepository;
    private final PgPaymentPort pgPaymentPort;
    private final ApplicationEventPublisher eventPublisher;

    // ──────────────────────────────────────────
    // 설정 CRUD
    // ──────────────────────────────────────────

    /**
     * 자동충전 설정 활성화·갱신.
     * 이미 존재하는 설정이면 갱신, 없으면 신규 생성.
     */
    @Transactional(rollbackFor = Exception.class)
    public AutoChargeDto.Response activate(Long memberId, AutoChargeDto.ActivateRequest request) {
        PaymentMethod paymentMethod = resolvePaymentMethod(memberId, request.paymentMethodId());

        if (paymentMethod.getPgBillingKey() == null || paymentMethod.getPgBillingKey().isBlank()) {
            throw new BillingException("자동충전은 빌링키가 등록된 결제수단만 사용 가능합니다.");
        }

        LocalDate expiresAt = request.apply30DayLimit() ? LocalDate.now().plusDays(30) : null;

        AutoChargeConfig config = autoChargeConfigRepository.findByMemberId(memberId)
                .map(existing -> {
                    existing.update(request.paymentMethodId(), request.thresholdAmount(),
                            request.chargeAmount(), expiresAt);
                    return existing;
                })
                .orElseGet(() -> AutoChargeConfig.builder()
                        .memberId(memberId)
                        .paymentMethodId(request.paymentMethodId())
                        .thresholdAmount(request.thresholdAmount())
                        .chargeAmount(request.chargeAmount())
                        .enabledYn("Y")
                        .expiresAt(expiresAt)
                        .build());

        AutoChargeConfig saved = autoChargeConfigRepository.save(config);
        log.info("[자동충전 활성화] memberId={} configId={} threshold={}원 charge={}원 expires={}",
                memberId, saved.getId(), saved.getThresholdAmount(), saved.getChargeAmount(), saved.getExpiresAt());

        return toResponse(saved);
    }

    /** 자동충전 설정 조회 */
    @Transactional(readOnly = true)
    public AutoChargeDto.Response getConfig(Long memberId) {
        AutoChargeConfig config = autoChargeConfigRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("AutoChargeConfig", memberId));
        return toResponse(config);
    }

    /** 자동충전 해지 */
    @Transactional(rollbackFor = Exception.class)
    public void deactivate(Long memberId) {
        AutoChargeConfig config = autoChargeConfigRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("AutoChargeConfig", memberId));
        config.disable();
        log.info("[자동충전 해지] memberId={} configId={}", memberId, config.getId());
    }

    // ──────────────────────────────────────────
    // 임계 도달 트리거 (내부 호출 / 스케줄러 호출 대상)
    // ──────────────────────────────────────────

    /**
     * 자동충전 트리거 — 현재 잔액이 임계치 이하이면 PG 정기결제 실행.
     *
     * 호출 시점: 발송 차감(ChargeService.deductForSend) 직후, 또는 주기적 배치.
     * 실패 시: CHARGE 행 FAILED 기록 + AutoChargeFailedEvent 발행.
     *
     * @param memberId       대상 회원 ID
     * @param currentBalance 현재 잔액 (DB 직접 조회값 권장 — 캐시 부정합 방지)
     */
    @Transactional(rollbackFor = Exception.class)
    public void triggerIfNeeded(Long memberId, long currentBalance) {
        AutoChargeConfig config = autoChargeConfigRepository.findByMemberId(memberId)
                .orElse(null);

        if (config == null) {
            return;
        }

        // 만료 체크 후 비활성화
        config.disableIfExpired();

        if (!config.shouldTrigger(currentBalance)) {
            return;
        }

        PaymentMethod paymentMethod = paymentMethodRepository.findById(config.getPaymentMethodId())
                .orElse(null);

        if (paymentMethod == null || !paymentMethod.isActive()
                || paymentMethod.getPgBillingKey() == null) {
            log.warn("[자동충전 스킵] memberId={} — 결제수단 없음 또는 빌링키 없음", memberId);
            return;
        }

        String idempotencyKey = "AUTO-" + memberId + "-" + UUID.randomUUID().toString().replace("-", "");

        PgPaymentPort.PgPaymentResult pgResult = pgPaymentPort.requestPayment(
                memberId,
                paymentMethod.getMethodType(),
                paymentMethod.getPgBillingKey(),
                config.getChargeAmount(),
                idempotencyKey
        );

        Charge charge = Charge.builder()
                .memberId(memberId)
                .paymentMethodId(paymentMethod.getId())
                .triggerId(config.getId())
                .chargeType(ChargeType.AUTO)
                .amount(config.getChargeAmount())
                .pgTxId(pgResult.success() ? pgResult.pgTxId() : "FAILED-" + idempotencyKey)
                .build();

        if (pgResult.success()) {
            charge.markSuccess();
            Charge savedCharge = chargeRepository.save(charge);

            LocalDateTime expiresAt = LocalDateTime.now().plusYears(5);
            ChargeBalance balance = ChargeBalance.builder()
                    .chargeId(savedCharge.getId())
                    .memberId(memberId)
                    .methodType(paymentMethod.getMethodType())
                    .amountInitial(config.getChargeAmount())
                    .expiresAt(expiresAt)
                    .build();
            chargeBalanceRepository.save(balance);

            // 잔액 캐시 무효화
            eventPublisher.publishEvent(new BalanceCacheEvictEvent(memberId));

            log.info("[자동충전 성공] memberId={} configId={} amount={}원 pgTxId={}",
                    memberId, config.getId(), config.getChargeAmount(), pgResult.pgTxId());
        } else {
            charge.markFailed(pgResult.failReason());
            chargeRepository.save(charge);

            // 실패 알림 이벤트 (AFTER_COMMIT)
            eventPublisher.publishEvent(new AutoChargeFailedEvent(
                    memberId, config.getId(), pgResult.failReason(), config.getChargeAmount()));

            log.warn("[자동충전 실패] memberId={} configId={} 사유={}",
                    memberId, config.getId(), pgResult.failReason());
        }
    }

    // ──────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────

    private PaymentMethod resolvePaymentMethod(Long memberId, Long paymentMethodId) {
        PaymentMethod pm = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new EntityNotFoundException("PaymentMethod", paymentMethodId));
        if (!pm.getMemberId().equals(memberId)) {
            throw new BillingException("본인의 결제수단이 아닙니다.");
        }
        if (!pm.isActive()) {
            throw new BillingException("비활성화된 결제수단입니다.");
        }
        return pm;
    }

    private AutoChargeDto.Response toResponse(AutoChargeConfig config) {
        return new AutoChargeDto.Response(
                config.getId(),
                config.getMemberId(),
                config.getPaymentMethodId(),
                config.getThresholdAmount(),
                config.getChargeAmount(),
                config.isEnabled(),
                config.getExpiresAt(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
