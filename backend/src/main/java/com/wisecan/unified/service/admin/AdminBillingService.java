package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.billing.BalanceLedgerReason;
import com.wisecan.unified.domain.billing.ChargeBalance;
import com.wisecan.unified.domain.billing.ChargeBalanceLedger;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.domain.billing.Refund;
import com.wisecan.unified.dto.admin.AdminBillingDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import com.wisecan.unified.repository.billing.RefundRepository;
import com.wisecan.unified.service.billing.BalanceCacheEvictEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 운영자 재무 관리 서비스 — §12.5.
 *
 * 환불 승인·반려는 W-404 RefundService 에 위임한다.
 * 이 서비스는 다음을 담당한다:
 *   - 운영자 전체 환불 목록 조회
 *   - 캐시 강제 적립/차감 (보상·이벤트, 사유 필수)
 *   - 정산 보고서 다운로드
 *
 * RQ-ADMIN-501~508
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBillingService {

    private final RefundRepository refundRepository;
    private final ChargeBalanceRepository chargeBalanceRepository;
    private final ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── 환불 목록 조회 ────────────────────────────────────────────────────

    /**
     * 운영자 전체 환불 목록 조회 — 최신순.
     * 회원 개별 조회와 달리 모든 회원의 환불 건을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AdminBillingDto.RefundSummary> listAllRefunds() {
        List<Refund> refunds = refundRepository.findAllByOrderByRequestedAtDesc();

        Set<Long> memberIds = refunds.stream()
                .map(Refund::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, String> emailByMemberId = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getEmail));

        return refunds.stream()
                .map(r -> new AdminBillingDto.RefundSummary(
                        r.getId(),
                        r.getMemberId(),
                        emailByMemberId.getOrDefault(r.getMemberId(), "-"),
                        r.getAmount(),
                        r.getMethodType(),
                        r.getStatus(),
                        r.isHasCashReceipt(),
                        r.getRequestedAt(),
                        r.getProcessedAt()))
                .toList();
    }

    // ── 캐시 강제 적립/차감 ───────────────────────────────────────────────

    /**
     * 캐시 강제 적립 또는 차감 — 보상·이벤트 목적.
     *
     * amount > 0 : 적립 (ADMIN_CREDIT)
     * amount < 0 : 차감 (ADMIN_DEBIT)
     *
     * 차감 시 잔액이 부족하면 예외를 던진다.
     * 단일 트랜잭션: ChargeBalance 변경 + 원장 기록 + 캐시 무효화 이벤트.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminBillingDto.CashAdjustResponse adjustCash(Long operatorId,
                                                          AdminBillingDto.CashAdjustRequest request) {
        if (request.amount() == 0) {
            throw new IllegalArgumentException("조정 금액은 0이 될 수 없습니다.");
        }

        if (!memberRepository.existsById(request.memberId())) {
            throw new EntityNotFoundException("Member", request.memberId());
        }

        long amount = request.amount();
        BalanceLedgerReason ledgerReason = amount > 0
                ? BalanceLedgerReason.ADMIN_CREDIT
                : BalanceLedgerReason.ADMIN_DEBIT;

        if (amount > 0) {
            // 적립: 가상 ChargeBalance 행 (methodType=ADMIN_GRANT, 만료 없음 = 50년)
            ChargeBalance cb = ChargeBalance.builder()
                    .chargeId(0L)
                    .memberId(request.memberId())
                    .methodType(PaymentMethodType.ADMIN_GRANT)
                    .amountInitial(amount)
                    .expiresAt(LocalDateTime.now().plusYears(50))
                    .build();
            chargeBalanceRepository.save(cb);

            ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                    .chargeBalanceId(cb.getId())
                    .operatorId(operatorId)
                    .amount(amount)
                    .reason(ledgerReason)
                    .build();
            chargeBalanceLedgerRepository.save(ledger);

            log.info("[캐시 강제 적립] memberId={} amount={} reason={} operatorId={}",
                    request.memberId(), amount, request.reason(), operatorId);
        } else {
            // 차감: 잔액에서 abs(amount) 만큼 차감 (FIFO — 만료 임박 우선)
            long remaining = Math.abs(amount);
            List<ChargeBalance> balances =
                    chargeBalanceRepository.findByMemberIdOrderByExpiresAtAscChargedAtAsc(request.memberId());

            for (ChargeBalance cb : balances) {
                if (remaining <= 0) break;
                if (!cb.hasBalance()) continue;

                long deducted = cb.deduct(remaining);
                remaining -= deducted;

                ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                        .chargeBalanceId(cb.getId())
                        .operatorId(operatorId)
                        .amount(-deducted)
                        .reason(ledgerReason)
                        .build();
                chargeBalanceLedgerRepository.save(ledger);
            }

            if (remaining > 0) {
                throw new IllegalStateException(
                        "잔액이 부족합니다. 부족분: " + remaining + "원");
            }

            log.info("[캐시 강제 차감] memberId={} amount={} reason={} operatorId={}",
                    request.memberId(), amount, request.reason(), operatorId);
        }

        eventPublisher.publishEvent(new BalanceCacheEvictEvent(request.memberId()));

        return new AdminBillingDto.CashAdjustResponse(
                request.memberId(),
                amount,
                request.reason(),
                operatorId,
                LocalDateTime.now());
    }

    // ── 정산 보고서 ───────────────────────────────────────────────────────

    /**
     * 정산 보고서 생성 — 기간별 회원별 집계.
     *
     * 현재 구현: ChargeBalance + ChargeBalanceLedger 기반 집계.
     * 실제 운영에서는 별도 집계 테이블(materialized view)로 대체할 수 있다.
     */
    @Transactional(readOnly = true)
    public AdminBillingDto.SettlementReport generateSettlementReport(
            LocalDateTime periodStart, LocalDateTime periodEnd) {

        List<ChargeBalance> allBalances = chargeBalanceRepository
                .findByChargedAtBetween(periodStart, periodEnd);

        Set<Long> memberIds = allBalances.stream()
                .map(ChargeBalance::getMemberId)
                .collect(Collectors.toSet());

        Map<Long, String> emailByMemberId = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getEmail));

        Map<Long, List<ChargeBalance>> byMember = allBalances.stream()
                .collect(Collectors.groupingBy(ChargeBalance::getMemberId));

        // H-2 수정: 기간 내 환불을 단일 IN 쿼리로 일괄 조회 후 memberId 로 그룹핑
        // (기존: 회원별 루프 내 개별 쿼리 → N+1 발생)
        Map<Long, Long> refundedByMember = refundRepository
                .findByMemberIdInAndRequestedAtBetween(memberIds, periodStart, periodEnd)
                .stream()
                .collect(Collectors.groupingBy(
                        Refund::getMemberId,
                        Collectors.summingLong(r -> r.getAmount() != null ? r.getAmount() : 0L)));

        long totalCharged = 0L;
        long totalRefunded = 0L;
        long totalDeducted = 0L;
        List<AdminBillingDto.SettlementRow> rows = new ArrayList<>();

        for (Map.Entry<Long, List<ChargeBalance>> entry : byMember.entrySet()) {
            Long memberId = entry.getKey();
            List<ChargeBalance> cbs = entry.getValue();

            long charged = cbs.stream().mapToLong(ChargeBalance::getAmountInitial).sum();
            long remaining = cbs.stream().mapToLong(ChargeBalance::getAmountRemaining).sum();
            long deducted = charged - remaining;
            long refunded = refundedByMember.getOrDefault(memberId, 0L);

            totalCharged += charged;
            totalRefunded += refunded;
            totalDeducted += deducted;

            rows.add(new AdminBillingDto.SettlementRow(
                    memberId,
                    emailByMemberId.getOrDefault(memberId, "-"),
                    charged,
                    refunded,
                    deducted,
                    remaining));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return new AdminBillingDto.SettlementReport(
                periodStart.format(fmt),
                periodEnd.format(fmt),
                totalCharged,
                totalRefunded,
                0L,
                totalDeducted,
                totalCharged - totalRefunded - totalDeducted,
                rows);
    }
}
