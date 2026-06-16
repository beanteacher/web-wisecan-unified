package com.wisecan.unified.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 실패 횟수를 Redis로 관리한다.
 * Redis 미사용 환경(로컬 테스트)에서는 Member 엔티티 필드로 대체된다.
 *
 * 스펙(RQ-AUTH-305~309): 5회 실패 → 15분 잠금 + CAPTCHA 요구
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final String KEY_PREFIX = "auth:fail:";
    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Nullable
    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(@Autowired(required = false) @Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 실패 횟수 증가. 반환값: 증가 후 현재 실패 횟수 */
    public int recordFailure(String email) {
        if (redisTemplate == null) {
            return 0;
        }
        String key = KEY_PREFIX + email;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 첫 실패 시 TTL 설정 (잠금 해제 시각 기준이 아니라 키 만료 기준)
                redisTemplate.expire(key, LOCK_DURATION);
            }
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to record login failure for {}: {}", email, e.getMessage());
            return 0;
        }
    }

    /** 실패 횟수 초기화 (로그인 성공 시) */
    public void resetFailure(String email) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(KEY_PREFIX + email);
        } catch (Exception e) {
            log.warn("Failed to reset login failure for {}: {}", email, e.getMessage());
        }
    }

    /** 현재 실패 횟수 조회 */
    public int getFailCount(String email) {
        if (redisTemplate == null) {
            return 0;
        }
        try {
            String val = redisTemplate.opsForValue().get(KEY_PREFIX + email);
            return val != null ? Integer.parseInt(val) : 0;
        } catch (Exception e) {
            log.warn("Failed to get fail count for {}: {}", email, e.getMessage());
            return 0;
        }
    }

    /** 잠금 상태인지 여부 (5회 이상 실패) */
    public boolean isLocked(String email) {
        return getFailCount(email) >= MAX_FAIL;
    }

    /** CAPTCHA 요구 여부 (3회 이상 실패) */
    public boolean isCaptchaRequired(String email) {
        return getFailCount(email) >= 3;
    }
}
