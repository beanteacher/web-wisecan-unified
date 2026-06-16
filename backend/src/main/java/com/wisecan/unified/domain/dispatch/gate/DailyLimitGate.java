package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 검증 4 — 일일 발송 한도.
 * API Key에 설정된 일일 한도(dailyLimit)가 있을 때 Redis 카운터로 검증한다.
 * Redis 키: quota:daily:{apiKeyId}:{yyyymmdd} — 익일 0시 만료.
 * 05_DATA_MODEL.md §5.6 / §4.2 API_KEY_LIMIT 참조.
 */
@Component
@RequiredArgsConstructor
public class DailyLimitGate implements SendValidationGate {

    private final ApiKeyRepository apiKeyRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void validate(SendValidationContext ctx) {
        ApiKey apiKey = apiKeyRepository.findById(ctx.apiKeyId())
                .orElseThrow(() -> new EntityNotFoundException("API Key를 찾을 수 없습니다."));

        Integer dailyLimit = apiKey.getDailyLimit();
        if (dailyLimit == null || dailyLimit == 0) {
            // 무제한
            return;
        }

        String today = LocalDate.now().toString().replace("-", "");
        String redisKey = "quota:daily:" + ctx.apiKeyId() + ":" + today;

        String currentStr = redisTemplate.opsForValue().get(redisKey);
        long current = currentStr == null ? 0L : Long.parseLong(currentStr);

        if (current + ctx.recipientCount() > dailyLimit) {
            throw new SendValidationException(SendErrorCode.DAILY_LIMIT_EXCEEDED,
                    "일일 발송 한도(" + dailyLimit + "건)를 초과합니다. 현재 누적: " + current + "건.");
        }
    }

    /** 익일 0시까지 남은 초 */
    static long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, midnight);
    }

    @Override
    public int order() {
        return 40;
    }
}
