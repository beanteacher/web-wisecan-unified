package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceGate — 잔액 사전 평가 검증")
class BalanceGateTest {

    @InjectMocks
    private BalanceGate gate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private JdbcTemplate jdbcTemplate;

    /** recipientCount × unitCost 로 totalCost 계산 */
    private SendValidationContext ctx(int recipientCount, long unitCost) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "안녕하세요", false, recipientCount, unitCost, NetworkType.TEST, null);
    }

    @Test
    @DisplayName("totalCost == 0 — 잔액 조회 없이 통과")
    void zeroCost_passes_withoutDbCall() {
        // recipientCount=1, unitCost=0 → totalCost=0
        SendValidationContext zeroCtx = new SendValidationContext(
                1L, 10L, ApiKeyType.TEST, "01012345678", SendChannel.SMS, "안녕하세요", false, 1, 0L, NetworkType.TEST, null);

        assertThatCode(() -> gate.validate(zeroCtx)).doesNotThrowAnyException();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("Redis 캐시 히트, 잔액 충분 — 통과")
    void redisCacheHit_sufficientBalance_passes() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn("10000");

        // totalCost = 10 × 10 = 100
        assertThatCode(() -> gate.validate(ctx(10, 10L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis 캐시 히트, 잔액 부족 — INSUFFICIENT_BALANCE")
    void redisCacheHit_insufficientBalance_throws() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn("50");

        // totalCost = 10 × 10 = 100 > 잔액 50
        assertThatThrownBy(() -> gate.validate(ctx(10, 10L)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("Redis 캐시 미스, DB 조회 후 잔액 충분 — 통과 및 캐시 갱신")
    void redisCacheMiss_dbSufficientBalance_passes() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn(null);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(5000L);

        // totalCost = 5 × 100 = 500 ≤ DB 잔액 5000
        assertThatCode(() -> gate.validate(ctx(5, 100L))).doesNotThrowAnyException();

        // 캐시 갱신 확인
        verify(valueOps).set(eq("balance:1"), eq("5000"), any());
    }

    @Test
    @DisplayName("Redis 캐시 미스, DB 조회 후 잔액 부족 — INSUFFICIENT_BALANCE")
    void redisCacheMiss_dbInsufficientBalance_throws() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn(null);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(30L);

        // totalCost = 5 × 100 = 500 > DB 잔액 30
        assertThatThrownBy(() -> gate.validate(ctx(5, 100L)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("DB 잔액 null 반환 — 0으로 간주, 잔액 부족")
    void dbReturnsNull_treatedAsZero_throws() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn(null);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(null);

        // totalCost = 1 × 10 = 10 > 잔액 0
        assertThatThrownBy(() -> gate.validate(ctx(1, 10L)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("잔액 == totalCost 정확히 일치 — 통과 (잔액 >= 비용)")
    void balanceExactlyEqualsCost_passes() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn("100");

        // totalCost = 10 × 10 = 100 == 잔액 100
        assertThatCode(() -> gate.validate(ctx(10, 10L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getBalance() — Redis 캐시 TTL 30초 설정 확인")
    void getBalance_setsCacheTtl30Seconds() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("balance:1")).willReturn(null);
        given(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(1L))).willReturn(999L);

        gate.getBalance(1L);

        verify(valueOps).set(
                eq("balance:1"),
                eq("999"),
                eq(java.time.Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("order() == 80")
    void order_is_80() {
        assertThat(gate.order()).isEqualTo(80);
    }
}
