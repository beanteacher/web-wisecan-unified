package com.wisecan.unified.service.billing;

import com.wisecan.unified.dto.billing.ChargeDto;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 잔액 조회 서비스 — W-401.
 *
 * Redis write-around 캐시 전략:
 *   - 캐시 HIT: balance:{memberId} 키 반환
 *   - 캐시 MISS: DB SUM 쿼리 → Redis 저장 (TTL 30s)
 *
 * 무효화는 BalanceCacheEvictListener (AFTER_COMMIT) 가 담당.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceQueryService {

    static final String BALANCE_KEY_PREFIX = "balance:";
    static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final ChargeBalanceRepository chargeBalanceRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 회원 잔액 조회 (캐시 우선) */
    @Transactional(readOnly = true)
    public ChargeDto.BalanceResponse getBalance(Long memberId) {
        String key = BALANCE_KEY_PREFIX + memberId;

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            long cachedBalance = Long.parseLong(cached.toString());
            log.debug("[잔액 캐시 HIT] memberId={} balance={}", memberId, cachedBalance);
            return new ChargeDto.BalanceResponse(memberId, cachedBalance, true);
        }

        Long dbBalance = chargeBalanceRepository.sumRemainingByMemberId(memberId, LocalDateTime.now());
        long balance = dbBalance != null ? dbBalance : 0L;

        redisTemplate.opsForValue().set(key, String.valueOf(balance), CACHE_TTL);
        log.debug("[잔액 DB 조회] memberId={} balance={}", memberId, balance);

        return new ChargeDto.BalanceResponse(memberId, balance, false);
    }

    /** 캐시를 통하지 않는 DB 직접 조회 — 정합성이 중요한 내부 로직용 */
    @Transactional(readOnly = true)
    public long getBalanceFromDb(Long memberId) {
        Long balance = chargeBalanceRepository.sumRemainingByMemberId(memberId, LocalDateTime.now());
        return balance != null ? balance : 0L;
    }
}
