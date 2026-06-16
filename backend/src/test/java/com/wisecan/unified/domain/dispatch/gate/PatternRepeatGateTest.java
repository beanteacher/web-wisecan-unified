package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.service.security.AbuseDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatternRepeatGate — 동일 메시지 패턴 반복 탐지")
class PatternRepeatGateTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AbuseDetectionService abuseDetectionService;

    private PatternRepeatGate gate;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        gate = new PatternRepeatGate(redisTemplate, abuseDetectionService);
    }

    private SendValidationContext ctx(String body) {
        return new SendValidationContext(1L, 10L, ApiKeyType.PRODUCTION, "01012345678",
                SendChannel.SMS, body, false, 1, 10L, NetworkType.PRODUCTION, null);
    }

    @Test
    @DisplayName("null 본문 — 검사 생략, 통과")
    void nullBody_skipped() {
        assertThatCode(() -> gate.validate(ctx(null))).doesNotThrowAnyException();
        then(valueOps).should(never()).get(anyString());
    }

    @Test
    @DisplayName("빈 본문 — 검사 생략, 통과")
    void blankBody_skipped() {
        assertThatCode(() -> gate.validate(ctx("   "))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("처음 발송 (카운터 null) — 통과")
    void firstSend_passes() {
        given(valueOps.get(anyString())).willReturn(null);
        given(valueOps.increment(anyString(), anyLong())).willReturn(1L);

        assertThatCode(() -> gate.validate(ctx("안녕하세요"))).doesNotThrowAnyException();
        then(abuseDetectionService).should(never()).recordAndBlock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("4번째 반복 — 임계값 이하, 통과")
    void fourthRepeat_passes() {
        given(valueOps.get(anyString())).willReturn("4");
        given(valueOps.increment(anyString(), anyLong())).willReturn(5L);

        assertThatCode(() -> gate.validate(ctx("동일 메시지"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("5번째 반복 — 임계값 초과, PATTERN_REPEAT_BLOCKED")
    void fifthRepeat_blocked() {
        given(valueOps.get(anyString())).willReturn("5");

        assertThatThrownBy(() -> gate.validate(ctx("동일 메시지")))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.PATTERN_REPEAT_BLOCKED));

        then(abuseDetectionService).should().recordAndBlock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sha256 — 동일 입력에 동일 해시 반환")
    void sha256_deterministic() {
        String h1 = PatternRepeatGate.sha256("테스트");
        String h2 = PatternRepeatGate.sha256("테스트");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    @DisplayName("sha256 — 다른 입력에 다른 해시 반환")
    void sha256_differentInput_differentHash() {
        String h1 = PatternRepeatGate.sha256("메시지A");
        String h2 = PatternRepeatGate.sha256("메시지B");
        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    @DisplayName("order() == 56")
    void order_is_56() {
        assertThat(gate.order()).isEqualTo(56);
    }
}
