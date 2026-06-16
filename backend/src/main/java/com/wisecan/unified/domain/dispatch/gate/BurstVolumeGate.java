package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.domain.security.AbuseRuleType;
import com.wisecan.unified.service.security.AbuseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 검증 5-A — 단시간 발송량 급증 탐지.
 * 슬라이딩 윈도우(60초) 내 수신자 수 합계가 임계값을 초과하면 차단한다.
 *
 * Redis 키: abuse:burst:{memberId} — TTL 60초 (슬라이딩 윈도우)
 * 임계값: 60초 내 1,000 수신자 (MVP 기본값, 운영 환경에서 조정)
 *
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-004 참조.
 */
@Component
@RequiredArgsConstructor
public class BurstVolumeGate implements SendValidationGate {

    /** 슬라이딩 윈도우 TTL */
    static final Duration WINDOW = Duration.ofSeconds(60);
    /** 60초 내 수신자 수 임계값 */
    static final long BURST_THRESHOLD = 1_000L;

    private final StringRedisTemplate redisTemplate;
    private final AbuseDetectionService abuseDetectionService;

    @Override
    public void validate(SendValidationContext ctx) {
        String key = "abuse:burst:" + ctx.memberId();

        // 현재 윈도우 누적 카운트 조회
        String currentStr = redisTemplate.opsForValue().get(key);
        long current = currentStr == null ? 0L : Long.parseLong(currentStr);

        long projected = current + ctx.recipientCount();

        if (projected > BURST_THRESHOLD) {
            // 이상 패턴 기록 및 자동 차단
            abuseDetectionService.recordAndBlock(
                    ctx.memberId(),
                    ctx.apiKeyId(),
                    AbuseRuleType.BURST_VOLUME,
                    String.format("60초 내 발송량 급증 감지: 누적 %d건 (임계 %d건)", projected, BURST_THRESHOLD),
                    projected,
                    BURST_THRESHOLD
            );
            throw new SendValidationException(SendErrorCode.BURST_VOLUME_EXCEEDED,
                    String.format("60초 내 발송량(%d건)이 임계값(%d건)을 초과했습니다.", projected, BURST_THRESHOLD));
        }

        // 발송 허용 시 카운터 누적 (키 없으면 새로 생성, TTL 초기화)
        Long newVal = redisTemplate.opsForValue().increment(key, ctx.recipientCount());
        if (newVal != null && newVal == ctx.recipientCount()) {
            // 새로 생성된 키에만 TTL 설정
            redisTemplate.expire(key, WINDOW);
        }
    }

    @Override
    public int order() {
        return 55;
    }
}
