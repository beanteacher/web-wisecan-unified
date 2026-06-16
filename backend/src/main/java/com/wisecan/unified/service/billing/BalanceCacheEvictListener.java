package com.wisecan.unified.service.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 잔액 캐시 무효화 리스너 — AFTER_COMMIT.
 * 충전/차감 트랜잭션 커밋 후 Redis 키를 삭제하여 stale read 방지.
 * TTL 30s write-around: 삭제 후 다음 조회 시 DB에서 재계산.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceCacheEvictListener {

    static final String BALANCE_KEY_PREFIX = "balance:";

    private final RedisTemplate<String, Object> redisTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBalanceCacheEvict(BalanceCacheEvictEvent event) {
        String key = BALANCE_KEY_PREFIX + event.memberId();
        Boolean deleted = redisTemplate.delete(key);
        log.debug("[잔액 캐시 무효화] key={} deleted={}", key, deleted);
    }
}
