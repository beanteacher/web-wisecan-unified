package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.BalanceLedgerReason;
import com.wisecan.unified.domain.billing.ChargeBalance;
import com.wisecan.unified.domain.billing.ChargeBalanceLedger;
import com.wisecan.unified.repository.billing.ChargeBalanceLedgerRepository;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 5년 미사용 잔액 소멸 배치 서비스 — W-404, 02_FEATURE_SPEC §10.4.
 *
 * 매일 01:00 에 만료된 ChargeBalance 를 일괄 소멸 처리한다.
 * 소멸 원장(EXPIRE)을 append-only 로 기록하고 Redis 캐시를 무효화한다.
 *
 * 사전 안내 정책:
 *   - 만료 30일 전, 7일 전, 1일 전 이메일/SMS 알림 (알림 서비스 연동 — 현재 로그만)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceExpireService {

    private final ChargeBalanceRepository chargeBalanceRepository;
    private final ChargeBalanceLedgerRepository chargeBalanceLedgerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 만료 잔액 소멸 배치 — 매일 01:00.
     * 만료된 모든 ChargeBalance 를 찾아 소멸 처리 후 원장 기록.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional(rollbackFor = Exception.class)
    public void expireBalances() {
        LocalDateTime now = LocalDateTime.now();
        List<ChargeBalance> expired = chargeBalanceRepository.findExpired(now);

        if (expired.isEmpty()) {
            log.info("[잔액 소멸 배치] 소멸 대상 없음");
            return;
        }

        log.info("[잔액 소멸 배치] 소멸 대상 {}건", expired.size());

        for (ChargeBalance cb : expired) {
            long expiredAmount = cb.getAmountRemaining();
            cb.expire();

            ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                    .chargeBalanceId(cb.getId())
                    .amount(-expiredAmount)
                    .reason(BalanceLedgerReason.EXPIRE)
                    .build();
            chargeBalanceLedgerRepository.save(ledger);
            eventPublisher.publishEvent(new BalanceCacheEvictEvent(cb.getMemberId()));

            log.info("[잔액 소멸] chargeBalanceId={} memberId={} amount={}",
                    cb.getId(), cb.getMemberId(), expiredAmount);
        }

        log.info("[잔액 소멸 배치] 완료 — 처리건수={}", expired.size());
    }

    /**
     * 테스트·운영자 수동 트리거용 — 특정 시점 기준 소멸.
     * 배치 외에도 운영자 콘솔에서 즉시 실행 가능.
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireBalancesAsOf(LocalDateTime asOf) {
        List<ChargeBalance> expired = chargeBalanceRepository.findExpired(asOf);
        int count = 0;
        for (ChargeBalance cb : expired) {
            long expiredAmount = cb.getAmountRemaining();
            cb.expire();
            ChargeBalanceLedger ledger = ChargeBalanceLedger.builder()
                    .chargeBalanceId(cb.getId())
                    .amount(-expiredAmount)
                    .reason(BalanceLedgerReason.EXPIRE)
                    .build();
            chargeBalanceLedgerRepository.save(ledger);
            eventPublisher.publishEvent(new BalanceCacheEvictEvent(cb.getMemberId()));
            count++;
        }
        return count;
    }
}
