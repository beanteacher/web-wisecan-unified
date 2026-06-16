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
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 검증 5-C — 비정상 시간대 대량 발송 탐지.
 * 새벽(00:00 ~ 06:00) 시간대에 단일 요청 수신자 수가 임계값(200건)을 초과하면 차단한다.
 * 비광고 메시지라도 새벽 시간대 대량 발송은 이상 패턴으로 간주한다.
 *
 * 주의: NightAdBlockGate(광고 야간 차단, order=70)와 별개 게이트.
 *       이 게이트는 메시지 유형에 무관하게 새벽 대량 발송 자체를 탐지한다.
 *
 * Redis 키: abuse:hour:{memberId}:{yyyymmddhh} — TTL 1시간
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-006 참조.
 */
@Component
@RequiredArgsConstructor
public class AnomalousHourGate implements SendValidationGate {

    /** 비정상 시간대 시작 (00:00) */
    static final LocalTime ANOMALOUS_START = LocalTime.MIDNIGHT;
    /** 비정상 시간대 종료 (06:00) */
    static final LocalTime ANOMALOUS_END = LocalTime.of(6, 0);
    /** 비정상 시간대 단일 요청 수신자 임계값 */
    static final int ANOMALOUS_THRESHOLD = 200;
    /** 시간대별 누적 임계값 */
    static final long HOURLY_THRESHOLD = 500L;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;
    private final AbuseDetectionService abuseDetectionService;

    @Override
    public void validate(SendValidationContext ctx) {
        LocalTime now = LocalTime.now(KST);
        if (!isAnomalousHour(now)) {
            return;
        }

        // 단일 요청 임계 초과
        if (ctx.recipientCount() > ANOMALOUS_THRESHOLD) {
            abuseDetectionService.recordAndBlock(
                    ctx.memberId(),
                    ctx.apiKeyId(),
                    AbuseRuleType.ANOMALOUS_HOUR,
                    String.format("새벽 시간대(%s) 단일 요청 %d건 감지 (임계 %d건)",
                            now, ctx.recipientCount(), ANOMALOUS_THRESHOLD),
                    (long) ctx.recipientCount(),
                    (long) ANOMALOUS_THRESHOLD
            );
            throw new SendValidationException(SendErrorCode.BURST_VOLUME_EXCEEDED,
                    String.format("새벽 시간대 대량 발송(%d건)이 감지되어 차단되었습니다.", ctx.recipientCount()));
        }

        // 시간대별 누적 임계 초과 체크
        String hourKey = "abuse:hour:" + ctx.memberId() + ":" + hourSlot();
        String currentStr = redisTemplate.opsForValue().get(hourKey);
        long current = currentStr == null ? 0L : Long.parseLong(currentStr);
        long projected = current + ctx.recipientCount();

        if (projected > HOURLY_THRESHOLD) {
            abuseDetectionService.recordAndBlock(
                    ctx.memberId(),
                    ctx.apiKeyId(),
                    AbuseRuleType.ANOMALOUS_HOUR,
                    String.format("새벽 시간대 누적 발송량 %d건 감지 (임계 %d건)", projected, HOURLY_THRESHOLD),
                    projected,
                    HOURLY_THRESHOLD
            );
            throw new SendValidationException(SendErrorCode.BURST_VOLUME_EXCEEDED,
                    String.format("새벽 시간대 누적 발송량(%d건)이 임계값을 초과했습니다.", projected));
        }

        Long newVal = redisTemplate.opsForValue().increment(hourKey, ctx.recipientCount());
        if (newVal != null && newVal == (long) ctx.recipientCount()) {
            redisTemplate.expire(hourKey, Duration.ofHours(1));
        }
    }

    /** 새벽 시간대(00:00 ~ 06:00) 여부 */
    static boolean isAnomalousHour(LocalTime time) {
        return !time.isBefore(ANOMALOUS_START) && time.isBefore(ANOMALOUS_END);
    }

    /** 현재 시간 슬롯 키 (yyyymmddhh) */
    private static String hourSlot() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now(KST);
        return String.format("%d%02d%02d%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    @Override
    public int order() {
        return 57;
    }
}
