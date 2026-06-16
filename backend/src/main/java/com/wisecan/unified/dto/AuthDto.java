package com.wisecan.unified.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class AuthDto {

    public record TermAgreementItem(
        @NotBlank String termCode,
        boolean agreed
    ) {}

    public record RegisterRequest(
        @NotBlank(message = "이메일은 필수입니다") @Email(message = "올바른 이메일 형식이 아닙니다") String email,
        @NotBlank(message = "비밀번호는 필수입니다") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String password,
        @NotBlank(message = "이름은 필수입니다") String name,
        String phone,
        @NotEmpty(message = "약관 동의 항목은 필수입니다") List<TermAgreementItem> termAgreements
    ) {}

    public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다") @Email(message = "올바른 이메일 형식이 아닙니다") String email,
        @NotBlank(message = "비밀번호는 필수입니다") String password
    ) {}

    public record TokenResponse(
        String accessToken,
        String refreshToken,
        String email,
        String name,
        String role
    ) {}

    /**
     * 2차 인증이 필요할 때 1차 로그인 응답으로 반환.
     * mfaToken: 임시 세션 토큰 (Redis에 저장, 5분 유효)
     * method: EMAIL | TOTP
     */
    public record MfaRequiredResponse(
        String mfaToken,
        String method,
        boolean captchaRequired
    ) {}

    /** 2차 인증 코드 검증 요청 */
    public record MfaVerifyRequest(
        @NotBlank(message = "MFA 토큰은 필수입니다") String mfaToken,
        @NotBlank(message = "인증 코드는 필수입니다") String code
    ) {}

    /** 2차 인증 설정 변경 요청 */
    public record TwoFactorSetupRequest(
        @NotBlank(message = "인증 수단(EMAIL|TOTP)은 필수입니다") String method,
        /** TOTP 설정 시 앱에서 생성된 코드로 검증 */
        String verificationCode
    ) {}

    /** 신뢰 IP 등록 요청 */
    public record TrustedIpRegisterRequest(
        @NotBlank(message = "IP 주소는 필수입니다") String ipAddress,
        String label
    ) {}

    /** 신뢰 IP 목록 항목 */
    public record TrustedIpItem(
        Long id,
        String ipAddress,
        String label,
        LocalDateTime createdAt
    ) {}

    /** 비밀번호 재설정 링크 요청 */
    public record PasswordResetRequest(
        @NotBlank(message = "이메일은 필수입니다") @Email String email
    ) {}

    /** 비밀번호 재설정 실행 */
    public record PasswordResetConfirmRequest(
        @NotBlank(message = "토큰은 필수입니다") String token,
        @NotBlank(message = "새 비밀번호는 필수입니다") @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다") String newPassword
    ) {}

    /** 아이디(이메일) 찾기 요청 */
    public record FindEmailRequest(
        @NotBlank(message = "이름은 필수입니다") String name,
        @NotBlank(message = "휴대폰 번호는 필수입니다") String phone
    ) {}

    /** 아이디 찾기 응답 */
    public record FindEmailResponse(
        String maskedEmail
    ) {}
}
