package com.wisecan.unified.service.billing;

import com.wisecan.unified.dto.billing.ChargeDto;
import com.wisecan.unified.repository.billing.ChargeBalanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceQueryServiceTest {

    @Mock ChargeBalanceRepository chargeBalanceRepository;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    @InjectMocks BalanceQueryService balanceQueryService;

    @Test
    @DisplayName("캐시 HIT — DB 조회 없이 캐시 값 반환")
    void getBalance_cacheHit_returnsFromCache() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("balance:100")).willReturn("30000");

        ChargeDto.BalanceResponse response = balanceQueryService.getBalance(100L);

        assertThat(response.totalBalance()).isEqualTo(30_000L);
        assertThat(response.fromCache()).isTrue();
        then(chargeBalanceRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("캐시 MISS — DB 조회 후 캐시 저장")
    void getBalance_cacheMiss_queriesDbAndCaches() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("balance:100")).willReturn(null);
        given(chargeBalanceRepository.sumRemainingByMemberId(eq(100L), any(LocalDateTime.class)))
                .willReturn(50_000L);

        ChargeDto.BalanceResponse response = balanceQueryService.getBalance(100L);

        assertThat(response.totalBalance()).isEqualTo(50_000L);
        assertThat(response.fromCache()).isFalse();
        then(valueOperations).should().set(eq("balance:100"), eq("50000"), any());
    }

    @Test
    @DisplayName("캐시 MISS + DB 잔액 0 — 0 반환 후 캐시 저장")
    void getBalance_cacheMiss_zeroBalance_returnsZero() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("balance:100")).willReturn(null);
        given(chargeBalanceRepository.sumRemainingByMemberId(eq(100L), any(LocalDateTime.class)))
                .willReturn(null);

        ChargeDto.BalanceResponse response = balanceQueryService.getBalance(100L);

        assertThat(response.totalBalance()).isEqualTo(0L);
        then(valueOperations).should().set(eq("balance:100"), eq("0"), any());
    }

    @Test
    @DisplayName("getBalanceFromDb — Redis 거치지 않고 DB 직접 조회")
    void getBalanceFromDb_bypasses_cache() {
        given(chargeBalanceRepository.sumRemainingByMemberId(eq(100L), any(LocalDateTime.class)))
                .willReturn(20_000L);

        long balance = balanceQueryService.getBalanceFromDb(100L);

        assertThat(balance).isEqualTo(20_000L);
        then(redisTemplate).shouldHaveNoInteractions();
    }
}
