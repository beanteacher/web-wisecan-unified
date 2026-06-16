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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 검증 5-B — 동일 메시지 패턴 반복 탐지.
 * 1분 내 동일 본문 SHA-256 해시가 5회 이상 발송되면 차단한다.
 *
 * Redis 키: abuse:pattern:{memberId}:{sha256(body)} — TTL 60초
 * 임계값: 60초 내 동일 본문 5회 (MVP 기본값)
 *
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-005 참조.
 */
@Component
@RequiredArgsConstructor
public class PatternRepeatGate implements SendValidationGate {

    static final Duration WINDOW = Duration.ofSeconds(60);
    static final long REPEAT_THRESHOLD = 5L;

    private final StringRedisTemplate redisTemplate;
    private final AbuseDetectionService abuseDetectionService;

    @Override
    public void validate(SendValidationContext ctx) {
        if (ctx.messageBody() == null || ctx.messageBody().isBlank()) {
            return;
        }

        String bodyHash = sha256(ctx.messageBody());
        String key = "abuse:pattern:" + ctx.memberId() + ":" + bodyHash;

        String currentStr = redisTemplate.opsForValue().get(key);
        long current = currentStr == null ? 0L : Long.parseLong(currentStr);

        long projected = current + 1;

        if (projected > REPEAT_THRESHOLD) {
            abuseDetectionService.recordAndBlock(
                    ctx.memberId(),
                    ctx.apiKeyId(),
                    AbuseRuleType.PATTERN_REPEAT,
                    String.format("동일 본문 패턴 반복 %d회 감지 (임계 %d회, hash=%s)", projected, REPEAT_THRESHOLD, bodyHash.substring(0, 8)),
                    projected,
                    REPEAT_THRESHOLD
            );
            throw new SendValidationException(SendErrorCode.PATTERN_REPEAT_BLOCKED,
                    String.format("동일 메시지 패턴이 %d회 반복 감지되어 차단되었습니다.", projected));
        }

        Long newVal = redisTemplate.opsForValue().increment(key, 1L);
        if (newVal != null && newVal == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
    }

    @Override
    public int order() {
        return 56;
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 Java 표준 — 발생하지 않음
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
