package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.repository.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyLimitGate — 일일 발송 한도 검증")
class DailyLimitGateTest {

    @InjectMocks
    private DailyLimitGate gate;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private String todayKey() {
        return "quota:daily:10:" + LocalDate.now().toString().replace("-", "");
    }

    private SendValidationContext ctx(int recipientCount) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "안녕하세요", false, recipientCount, 10L, NetworkType.TEST);
    }

    private ApiKey keyWithLimit(Integer dailyLimit) {
        return ApiKey.builder()
                .keyName("테스트키").keyPrefix("wc_test").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.TEST)
                .scopes(Set.of(ApiKeyScope.SEND_SMS))
                .dailyLimit(dailyLimit).build();
    }

    @Test
    @DisplayName("dailyLimit null — 무제한 통과 (Redis 조회 없음)")
    void nullLimit_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(null)));

        assertThatCode(() -> gate.validate(ctx(9999))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dailyLimit 0 — 무제한 통과")
    void zeroLimit_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(0)));

        assertThatCode(() -> gate.validate(ctx(9999))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("현재 카운터 0, 한도 100, 수신자 50 — 통과")
    void withinLimit_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(100)));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(todayKey())).willReturn("0");

        assertThatCode(() -> gate.validate(ctx(50))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("현재 카운터 90, 한도 100, 수신자 11 — DAILY_LIMIT_EXCEEDED")
    void exceedsLimit_throws() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(100)));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(todayKey())).willReturn("90");

        assertThatThrownBy(() -> gate.validate(ctx(11)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("Redis 키 없음(null) — 현재 0으로 간주, 한도 이내면 통과")
    void redisNull_treatedAsZero_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(100)));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn(null);

        assertThatCode(() -> gate.validate(ctx(100))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("현재 카운터 = 한도 정확히 일치 — DAILY_LIMIT_EXCEEDED (초과)")
    void exactLimit_throws() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithLimit(100)));
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(todayKey())).willReturn("100");

        // current(100) + recipient(1) = 101 > limit(100) → 초과
        assertThatThrownBy(() -> gate.validate(ctx(1)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.DAILY_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("order() == 40")
    void order_is_40() {
        assertThat(gate.order()).isEqualTo(40);
    }
}
