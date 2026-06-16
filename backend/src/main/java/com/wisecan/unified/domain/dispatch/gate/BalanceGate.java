package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 검증 8 — 잔액 사전 평가.
 * 총 차감 예정액({@code recipientCount × unitCost})이 현재 잔액 이하여야 한다.
 *
 * 잔액 조회 hot path:
 *   1. Redis GET balance:{memberId} (TTL 30초)
 *   2. Redis MISS 시 CHARGE_BALANCE 테이블 SUM(amount_remaining) 조회
 *
 * 05_DATA_MODEL.md §7.2 / §5.6 참조.
 * 잔액 부족 시 {@link SendErrorCode#INSUFFICIENT_BALANCE}를 던진다.
 * 자동충전/후불 분기(02 §11)는 상위 서비스(SendValidationService)가 담당한다.
 */
@Component
@RequiredArgsConstructor
public class BalanceGate implements SendValidationGate {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void validate(SendValidationContext ctx) {
        long totalCost = ctx.totalCost();
        if (totalCost <= 0) {
            return;
        }

        long balance = getBalance(ctx.memberId());
        if (balance < totalCost) {
            throw new SendValidationException(SendErrorCode.INSUFFICIENT_BALANCE,
                    "잔액이 부족합니다. 현재 잔액: " + balance + "원, 필요 금액: " + totalCost + "원.");
        }
    }

    /**
     * 잔액 조회 — Redis 캐시 우선, 미스 시 DB 직접 조회.
     */
    long getBalance(Long memberId) {
        String cacheKey = "balance:" + memberId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Long.parseLong(cached);
        }

        // Redis 미스 — CHARGE_BALANCE 테이블에서 만료 전 잔액 합산
        Long dbBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_remaining), 0) FROM charge_balance " +
                "WHERE member_id = ? AND expires_at > NOW() AND amount_remaining > 0",
                Long.class,
                memberId);

        long balance = dbBalance != null ? dbBalance : 0L;

        // 캐시 갱신 (TTL 30초, write-around 패턴)
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(balance),
                java.time.Duration.ofSeconds(30));

        return balance;
    }

    @Override
    public int order() {
        return 80;
    }
}
