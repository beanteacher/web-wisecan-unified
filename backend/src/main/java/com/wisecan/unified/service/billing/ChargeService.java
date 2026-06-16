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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 충전 핵심 서비스 — W-401.
 *
 * 충전 흐름:
 *   1. BILLING_MODE 검증 (POSTPAID 회원은 수동 충전 불가)
 *   2. 결제수단 유효성 확인
 *   3. PG 결제 요청 (StubPgPaymentAdapter — pg.stub=true)
 *   4. CHARGE 행 SUCCESS 확정
 *   5. CHARGE_BALANCE 행 적립 (expires_at = now + 5년)
 *   6. Redis 캐시 무효화 (write-around, AFTER_COMMIT 이벤트)
 *
 * 멱등성: pgTxId UNIQUE 제약으로 PG 중복 콜백 차단.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChargeService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeBalanceRepository chargeBalanceRepository;
    private final ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    private final PgPaymentPort pgPaymentPort;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;

    /** 수동 충전 — POST /billing/charge */
    @Transactional(rollbackFor = Exception.class)
    public ChargeDto.Response charge(Long memberId, ChargeDto.CreateRequest request) {
        validatePrepaidMode(resolveBillingMode(memberId));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                .orElseThrow(() -> new EntityNotFoundException("결제수단을 찾을 수 없습니다: " + request.paymentMethodId()));

        if (!paymentMethod.getMemberId().equals(memberId)) {
            throw new BillingException("본인의 결제수단이 아닙니다.");
        }
        if (!paymentMethod.isActive()) {
            throw new BillingException("비활성화된 결제수단입니다.");
        }

        String idempotencyKey = "CHARGE-" + memberId + "-" + UUID.randomUUID().toString().replace("-", "");

        PgPaymentPort.PgPaymentResult pgResult = pgPaymentPort.requestPayment(
                memberId,
                paymentMethod.getMethodType(),
                paymentMethod.getPgBillingKey(),
                request.amount(),
                idempotencyKey
        );

        // CHARGE 행 생성 (REQUESTED 상태)
        Charge charge = Charge.builder()
                .memberId(memberId)
                .paymentMethodId(paymentMethod.getId())
                .chargeType(ChargeType.MANUAL)
                .amount(request.amount())
                .pgTxId(pgResult.success() ? pgResult.pgTxId() : "FAILED-" + idempotencyKey)
                .build();

        if (pgResult.success()) {
            charge.markSuccess();
            charge = chargeRepository.save(charge);

            // CHARGE_BALANCE 적립 (예수금, 결제수단별 분리)
            LocalDateTime expiresAt = LocalDateTime.now().plusYears(5);
            ChargeBalance balance = ChargeBalance.builder()
                    .chargeId(charge.getId())
                    .memberId(memberId)
                    .methodType(paymentMethod.getMethodType())
                    .amountInitial(request.amount())
                    .expiresAt(expiresAt)
                    .build();
            chargeBalanceRepository.save(balance);

            // Redis 캐시 무효화 이벤트 발행 (AFTER_COMMIT)
            eventPublisher.publishEvent(new BalanceCacheEvictEvent(memberId));

            log.info("[충전 완료] memberId={} amount={} pgTxId={}", memberId, request.amount(), pgResult.pgTxId());
        } else {
            charge.markFailed(pgResult.failReason());
            charge = chargeRepository.save(charge);
            throw new BillingException("결제 실패: " + pgResult.failReason());
        }

        return toResponse(charge, paymentMethod.getMethodType());
    }

    /** 회원의 청구 모드 해석 — 개인회원(companyId 없음)은 PREPAID, 회사 소속이면 Company.billingMode */
    private String resolveBillingMode(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId));
        if (member.getCompanyId() == null) {
            return "PREPAID";
        }
        return companyRepository.findById(member.getCompanyId())
                .map(Company::getBillingMode)
                .orElse("PREPAID");
    }

    /** POSTPAID 회원은 수동 충전 불가 */
    private void validatePrepaidMode(String billingMode) {
        if ("POSTPAID".equals(billingMode)) {
            throw new BillingException("후불 정산 회원은 수동 충전을 이용할 수 없습니다.");
        }
    }

    /**
     * 발송 차감 — FIFO + 만료 임박 우선.
     * 외부 발송 ID 기반 멱등성 보장.
     *
     * @param memberId       차감 대상 회원
     * @param amount         차감 금액
     * @param externalSendId 외부 발송 ID (멱등성 키)
     * @return 실제 차감된 총액
     */
    @Transactional(rollbackFor = Exception.class)
    public long deductForSend(Long memberId, long amount, String externalSendId) {
        // 멱등성 — 이미 처리된 발송이면 스킵
        if (chargeBalanceLedgerRepository
                .findByExternalSendIdAndReason(externalSendId, BalanceLedgerReason.SEND)
                .isPresent()) {
            log.info("[발송 차감 중복] externalSendId={}", externalSendId);
            return amount;
        }

        List<ChargeBalance> balances = chargeBalanceRepository
                .findDeductibleByMemberId(memberId, LocalDateTime.now());

        long remaining = amount;
        for (ChargeBalance cb : balances) {
            if (remaining <= 0) break;
            long deducted = cb.deduct(remaining);
            if (deducted > 0) {
                ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                        .chargeBalanceId(cb.getId())
                        .amount(-deducted)
                        .reason(BalanceLedgerReason.SEND)
                        .externalSendId(externalSendId)
                        .build();
                chargeBalanceLedgerRepository.save(ledger);
                remaining -= deducted;
            }
        }

        if (remaining > 0) {
            throw new BillingException("잔액이 부족합니다. 부족액: " + remaining + "원");
        }

        eventPublisher.publishEvent(new BalanceCacheEvictEvent(memberId));
        return amount;
    }

    private ChargeDto.Response toResponse(Charge charge, PaymentMethodType methodType) {
        return new ChargeDto.Response(
                charge.getId(),
                charge.getMemberId(),
                methodType,
                charge.getAmount(),
                charge.getChargeType(),
                charge.getStatus(),
                charge.getPgTxId(),
                charge.getPaidAt(),
                charge.getCreatedAt()
        );
    }
}
