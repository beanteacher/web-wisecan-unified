package com.wisecan.unified.service.billing;

import com.wisecan.unified.repository.billing.AutoChargeConfigRepository;
import com.wisecan.unified.repository.billing.PostpaidConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 잔액 부족 분기 라우터 — W-405.
 *
 * 02_FEATURE_SPEC §11.1 정상 흐름:
 *   잔액 - 차감 예정액 < 0 이면
 *   1) 자동결제 활성 → AutoChargeService.triggerIfNeeded() → AutoCharged
 *   2) 후불 활성      → Postpaid (청구서 누적은 정산 배치 담당)
 *   3) 둘 다 비활성  → Partial(부분 발송) 또는 Cancelled(전체 취소)
 *
 * 호출 시점: SendRequestService / WebSendService 에서 BalanceGate 실패 후
 * (SendValidationException 을 catch 하지 않고, 발송 서비스가 이 라우터를 먼저 호출하는 구조).
 *
 * 후불 분기는 companyId 가 필요하다. 개인 회원(companyId=null) 은 후불 분기를 건너뛴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InsufficientBalanceRouter {

    private final AutoChargeConfigRepository autoChargeConfigRepository;
    private final PostpaidConfigRepository postpaidConfigRepository;
    private final AutoChargeService autoChargeService;
    private final BalanceQueryService balanceQueryService;

    /**
     * 잔액 부족 분기를 평가하고 결과를 반환한다.
     *
     * @param memberId       발송 요청자 회원 ID
     * @param companyId      소속 회사 ID (개인 회원이면 null)
     * @param totalCost      총 차감 예정액 (recipientCount × unitCost)
     * @param allRecipients  전체 수신자 번호 목록 (부분 발송 계산에 사용)
     * @param unitCost       건당 단가
     * @param partialChoice  true = 부분 발송 선택, false = 전체 취소 선택
     *                       (분기 1·2 해당 없을 때만 유효)
     * @return 분기 결과
     */
    @Transactional(rollbackFor = Exception.class)
    public BalanceBranchResult route(
            Long memberId,
            Long companyId,
            long totalCost,
            List<String> allRecipients,
            long unitCost,
            boolean partialChoice
    ) {
        long currentBalance = balanceQueryService.getBalanceFromDb(memberId);
        long shortfall = totalCost - currentBalance;

        if (shortfall <= 0) {
            // 호출 시점 재조회 결과 잔액 충분 — 자동결제 불필요
            log.debug("[W-405] 재조회 시 잔액 충분 memberId={} balance={} totalCost={}",
                    memberId, currentBalance, totalCost);
            return new BalanceBranchResult.AutoCharged(0L);
        }

        // ── 분기 1: 자동결제 활성 ──────────────────────────────────────
        boolean autoChargeActive = autoChargeConfigRepository.findByMemberId(memberId)
                .map(cfg -> {
                    cfg.disableIfExpired();
                    return cfg.isEnabled();
                })
                .orElse(false);

        if (autoChargeActive) {
            log.info("[W-405] 자동결제 트리거 memberId={} shortfall={}", memberId, shortfall);
            autoChargeService.triggerIfNeeded(memberId, currentBalance);
            long afterBalance = balanceQueryService.getBalanceFromDb(memberId);
            long chargedAmount = afterBalance - currentBalance;
            log.info("[W-405] 자동결제 완료 memberId={} chargedAmount={} afterBalance={}",
                    memberId, chargedAmount, afterBalance);
            return new BalanceBranchResult.AutoCharged(chargedAmount);
        }

        // ── 분기 2: 후불 활성 ─────────────────────────────────────────
        if (companyId != null) {
            boolean postpaidActive = postpaidConfigRepository.findByCompanyId(companyId)
                    .map(cfg -> cfg.isActive())
                    .orElse(false);

            if (postpaidActive) {
                log.info("[W-405] 후불 처리 companyId={} deferredAmount={}", companyId, totalCost);
                return new BalanceBranchResult.Postpaid(totalCost);
            }
        }

        // ── 분기 3·4: 부분 발송 / 전체 취소 ───────────────────────────
        if (partialChoice && unitCost > 0 && !allRecipients.isEmpty()) {
            int affordableCount = (int) (currentBalance / unitCost);
            if (affordableCount <= 0) {
                log.info("[W-405] 잔액 0원 미만 — 전체 취소 memberId={}", memberId);
                return new BalanceBranchResult.Cancelled(shortfall);
            }
            List<String> accepted = allRecipients.subList(0, affordableCount);
            List<String> rejected = allRecipients.subList(affordableCount, allRecipients.size());
            log.info("[W-405] 부분 발송 memberId={} accepted={} rejected={} shortfall={}",
                    memberId, accepted.size(), rejected.size(), shortfall);
            return new BalanceBranchResult.Partial(
                    accepted.size(),
                    rejected.size(),
                    List.copyOf(rejected),
                    shortfall
            );
        }

        log.info("[W-405] 전체 취소 memberId={} shortfall={}", memberId, shortfall);
        return new BalanceBranchResult.Cancelled(shortfall);
    }
}
