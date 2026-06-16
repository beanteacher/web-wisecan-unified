package com.wisecan.unified.controller;

import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.exception.TwoFactorRequiredException;
import com.wisecan.unified.service.AuthService;
import com.wisecan.unified.service.TrustedIpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TrustedIpService trustedIpService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> register(
            @RequestBody @Valid AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(authService.register(request)));
    }

    /**
     * 로그인 (§1.3).
     * 2차 인증이 필요한 경우 HTTP 202 + MfaRequiredResponse 반환.
     * 2차 인증 불필요이면 HTTP 200 + TokenResponse 반환.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(
            @RequestBody @Valid AuthDto.LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        try {
            AuthDto.TokenResponse tokens = authService.login(request, clientIp);
            return ResponseEntity.ok(ApiResponse.success(tokens));
        } catch (TwoFactorRequiredException ex) {
            boolean captchaRequired = false; // LoginAttemptService에서 필요 시 주입 가능
            AuthDto.MfaRequiredResponse mfaResponse =
                new AuthDto.MfaRequiredResponse(ex.getMfaToken(), "EMAIL", captchaRequired);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(mfaResponse));
        }
    }

    /**
     * 2차 인증 코드 검증 후 최종 토큰 발급 (§1.3).
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> verifyMfa(
            @RequestBody @Valid AuthDto.MfaVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyMfa(request)));
    }

    /**
     * 로그아웃 — 현재 액세스 토큰을 블랙리스트에 등록.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(extractBearer(authHeader));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 비밀번호 재설정 링크 요청 (§1.4 RQ-AUTH-303).
     */
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @RequestBody @Valid AuthDto.PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 비밀번호 재설정 실행 (§1.4 RQ-AUTH-304).
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(
            @RequestBody @Valid AuthDto.PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 아이디(이메일) 찾기 (§1.4 RQ-AUTH-303).
     */
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<AuthDto.FindEmailResponse>> findEmail(
            @RequestBody @Valid AuthDto.FindEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.findEmail(request)));
    }

    // ── 신뢰 IP 관리 (인증 필요) ────────────────────────────────────────────

    /**
     * 신뢰 IP 목록 조회.
     */
    @GetMapping("/trusted-ips")
    public ResponseEntity<ApiResponse<List<AuthDto.TrustedIpItem>>> listTrustedIps(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
            trustedIpService.list(principal.memberId())
        ));
    }

    /**
     * 신뢰 IP 등록.
     */
    @PostMapping("/trusted-ips")
    public ResponseEntity<ApiResponse<AuthDto.TrustedIpItem>> registerTrustedIp(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid AuthDto.TrustedIpRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                trustedIpService.register(principal.memberId(), request)
            ));
    }

    /**
     * 신뢰 IP 삭제.
     */
    @DeleteMapping("/trusted-ips/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrustedIp(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        trustedIpService.delete(principal.memberId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String extractBearer(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /** X-Forwarded-For 헤더 우선, 없으면 RemoteAddr 사용 */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
