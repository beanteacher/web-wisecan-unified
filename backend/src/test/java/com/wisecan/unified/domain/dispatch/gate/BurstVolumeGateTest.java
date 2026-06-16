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
@DisplayName("BurstVolumeGate — 단시간 발송량 급증 탐지")
class BurstVolumeGateTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AbuseDetectionService abuseDetectionService;

    private BurstVolumeGate gate;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        gate = new BurstVolumeGate(redisTemplate, abuseDetectionService);
    }

    private SendValidationContext ctx(int recipientCount) {
        return new SendValidationContext(1L, 10L, ApiKeyType.PRODUCTION, "01012345678",
                SendChannel.SMS, "테스트 메시지", false, recipientCount, 10L, NetworkType.PRODUCTION, null);
    }

    @Test
    @DisplayName("누적 없음 + 소량 발송 — 통과")
    void noAccumulation_smallBatch_passes() {
        given(valueOps.get(anyString())).willReturn(null);
        given(valueOps.increment(anyString(), anyLong())).willReturn(100L);

        assertThatCode(() -> gate.validate(ctx(100))).doesNotThrowAnyException();
        then(abuseDetectionService).should(never()).recordAndBlock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("누적 900 + 요청 100 = 1000 — 임계값 이하, 통과")
    void accumulated900_plus100_passes() {
        given(valueOps.get(anyString())).willReturn("900");
        given(valueOps.increment(anyString(), anyLong())).willReturn(1000L);

        assertThatCode(() -> gate.validate(ctx(100))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("누적 900 + 요청 101 = 1001 — 임계값 초과, BURST_VOLUME_EXCEEDED")
    void accumulated900_plus101_throwsBurst() {
        given(valueOps.get(anyString())).willReturn("900");

        assertThatThrownBy(() -> gate.validate(ctx(101)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.BURST_VOLUME_EXCEEDED));

        then(abuseDetectionService).should().recordAndBlock(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("누적 0 + 요청 1001 — 단일 요청 임계 초과, 차단")
    void singleRequestOverThreshold_blocked() {
        given(valueOps.get(anyString())).willReturn("0");

        assertThatThrownBy(() -> gate.validate(ctx(1001)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.BURST_VOLUME_EXCEEDED));
    }

    @Test
    @DisplayName("order() == 55")
    void order_is_55() {
        assertThat(gate.order()).isEqualTo(55);
    }
}
