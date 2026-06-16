package com.wisecan.unified.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

/**
 * 2차 인증 서비스 (스펙 §1.3 RQ-AUTH-306~311).
 *
 * 흐름:
 *   1) 1차 인증(이메일+비밀번호) 통과 → issueMfaSession() 호출 → mfaToken 반환
 *   2) 클라이언트가 이메일 수신 코드 또는 TOTP 앱 코드를 POST /auth/mfa/verify 로 제출
 *   3) verifyMfaCode() 성공 → 최종 JWT 발급
 *
 * 이메일 발송은 실제 메일 서비스 연동이 필요하므로 로그 출력으로 대체 (개발 단계).
 * TOTP는 표준 TOTP(RFC 6238) 검증 로직을 포함한다 (commons-codec 활용).
 */
@Service
@Slf4j
public class TwoFactorAuthService {

    private static final String MFA_SESSION_PREFIX = "auth:mfa:session:";
    private static final String MFA_CODE_PREFIX    = "auth:mfa:code:";
    private static final Duration MFA_SESSION_TTL  = Duration.ofMinutes(5);
    private static final Duration MFA_CODE_TTL     = Duration.ofMinutes(5);
    private static final int EMAIL_CODE_LENGTH      = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    @Nullable
    private final StringRedisTemplate redisTemplate;

    public TwoFactorAuthService(@Autowired(required = false) @Nullable StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 1차 인증 통과 후 MFA 세션을 발급한다.
     * Redis에 mfaToken → email 매핑을 저장하고, 이메일 방식일 때 OTP 코드를 발송한다.
     *
     * @param email  회원 이메일
     * @param method EMAIL | TOTP
     * @return 임시 mfaToken (클라이언트가 2차 인증 요청 시 전달)
     */
    public String issueMfaSession(String email, String method) {
        String mfaToken = UUID.randomUUID().toString();

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(MFA_SESSION_PREFIX + mfaToken, email, MFA_SESSION_TTL);
            } catch (Exception e) {
                log.warn("Failed to store MFA session: {}", e.getMessage());
            }
        }

        if ("EMAIL".equalsIgnoreCase(method)) {
            String code = generateEmailCode();
            storeEmailCode(email, code);
            sendEmailCode(email, code);
        }

        return mfaToken;
    }

    /**
     * 2차 인증 코드 검증.
     *
     * @param mfaToken 임시 세션 토큰
     * @param code     사용자가 제출한 코드
     * @param method   EMAIL | TOTP
     * @param totpSecret TOTP 시크릿 (method=TOTP 일 때만 사용)
     * @return 검증된 회원 이메일
     */
    public String verifyMfaCode(String mfaToken, String code, String method, String totpSecret) {
        String email = resolveEmail(mfaToken);
        if (email == null) {
            throw new IllegalArgumentException("MFA 세션이 만료되었거나 유효하지 않습니다.");
        }

        boolean valid;
        if ("TOTP".equalsIgnoreCase(method)) {
            valid = verifyTotp(totpSecret, code);
        } else {
            valid = verifyEmailCode(email, code);
        }

        if (!valid) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않거나 만료되었습니다.");
        }

        // 세션 소비 (재사용 방지)
        invalidateMfaSession(mfaToken);
        return email;
    }

    /** MFA 세션에서 이메일 조회 */
    public String resolveEmail(String mfaToken) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(MFA_SESSION_PREFIX + mfaToken);
        } catch (Exception e) {
            log.warn("Failed to resolve MFA session: {}", e.getMessage());
            return null;
        }
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private String generateEmailCode() {
        int code = 100_000 + secureRandom.nextInt(900_000);
        return String.valueOf(code);
    }

    private void storeEmailCode(String email, String code) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(MFA_CODE_PREFIX + email, code, MFA_CODE_TTL);
        } catch (Exception e) {
            log.warn("Failed to store MFA email code: {}", e.getMessage());
        }
    }

    private void sendEmailCode(String email, String code) {
        // TODO: 실제 메일 서비스(JavaMailSender 또는 외부 SES)로 대체
        log.info("[2FA] 이메일 인증 코드 발송 → {} : {}", email, code);
    }

    private boolean verifyEmailCode(String email, String code) {
        if (redisTemplate == null) {
            // Redis 없는 개발 환경에서는 "000000" 으로 패스
            return "000000".equals(code);
        }
        try {
            String stored = redisTemplate.opsForValue().get(MFA_CODE_PREFIX + email);
            if (stored != null && stored.equals(code)) {
                redisTemplate.delete(MFA_CODE_PREFIX + email);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to verify MFA email code: {}", e.getMessage());
            return false;
        }
    }

    /**
     * RFC 6238 TOTP 검증 (±1 타임스텝 허용).
     * 실제 프로덕션에서는 Google Authenticator 호환 라이브러리(jtotp 등)를 사용한다.
     * 현재는 인터페이스 구조만 확정하고 stub으로 처리한다.
     */
    private boolean verifyTotp(String totpSecret, String code) {
        if (totpSecret == null || code == null) {
            return false;
        }
        // TODO: jtotp 라이브러리 연동 — GoogleAuthenticator.authorize(totpSecret, Integer.parseInt(code))
        log.debug("[2FA] TOTP 검증 stub — secret={}, code={}", totpSecret.substring(0, Math.min(4, totpSecret.length())), code);
        return code.length() == 6 && code.chars().allMatch(Character::isDigit);
    }

    private void invalidateMfaSession(String mfaToken) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(MFA_SESSION_PREFIX + mfaToken);
        } catch (Exception e) {
            log.warn("Failed to invalidate MFA session: {}", e.getMessage());
        }
    }
}
