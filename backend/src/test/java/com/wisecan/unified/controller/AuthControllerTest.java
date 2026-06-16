package com.wisecan.unified.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.exception.AccountLockedException;
import com.wisecan.unified.exception.DuplicateEmailException;
import com.wisecan.unified.exception.TwoFactorRequiredException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.service.AuthService;
import com.wisecan.unified.service.TokenBlacklistService;
import com.wisecan.unified.service.TrustedIpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TrustedIpService trustedIpService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private List<AuthDto.TermAgreementItem> sampleTermAgreements() {
        return List.of(
            new AuthDto.TermAgreementItem("TOS", true),
            new AuthDto.TermAgreementItem("PRIVACY", true)
        );
    }

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공 - 201 Created")
    @WithMockUser
    void register_success() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "user@test.com", "password123", "홍길동", null,
            sampleTermAgreements()
        );
        AuthDto.TokenResponse response = new AuthDto.TokenResponse(
            "accessToken", "refreshToken", "user@test.com", "홍길동", "MEMBER"
        );

        given(authService.register(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
            .andExpect(jsonPath("$.data.email").value("user@test.com"))
            .andExpect(jsonPath("$.data.role").value("MEMBER"));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 - 409 Conflict")
    @WithMockUser
    void register_duplicateEmail_conflict() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "dup@test.com", "password123", "홍길동", null,
            sampleTermAgreements()
        );

        given(authService.register(any())).willThrow(new DuplicateEmailException("dup@test.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("약관 동의 없이 회원가입 - 400 Bad Request")
    @WithMockUser
    void register_noTermAgreements_badRequest() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "user@test.com", "password123", "홍길동", null, List.of()
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 - 200 OK")
    @WithMockUser
    void login_success() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");
        AuthDto.TokenResponse response = new AuthDto.TokenResponse(
            "accessToken", "refreshToken", "user@test.com", "홍길동", "MEMBER"
        );

        given(authService.login(any(), anyString())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("2차 인증 필요 - 202 Accepted + mfaToken 반환")
    @WithMockUser
    void login_mfaRequired_returns202() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");

        given(authService.login(any(), anyString()))
            .willThrow(new TwoFactorRequiredException("mfa-token-abc"));

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.mfaToken").value("mfa-token-abc"));
    }

    @Test
    @DisplayName("계정 잠금 상태 로그인 - 429 Too Many Requests")
    @WithMockUser
    void login_accountLocked_returns429() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");

        given(authService.login(any(), anyString()))
            .willThrow(new AccountLockedException());

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("15분")));
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 - 400 Bad Request")
    @WithMockUser
    void login_wrongPassword_badRequest() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "wrongPassword");

        given(authService.login(any(), anyString()))
            .willThrow(new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다"));

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── 2차 인증 검증 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("2차 인증 코드 검증 성공 - 200 OK + 토큰 발급")
    @WithMockUser
    void verifyMfa_success() throws Exception {
        AuthDto.MfaVerifyRequest request = new AuthDto.MfaVerifyRequest("mfa-token-abc", "123456");
        AuthDto.TokenResponse response = new AuthDto.TokenResponse(
            "accessToken", "refreshToken", "user@test.com", "홍길동", "MEMBER"
        );

        given(authService.verifyMfa(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("만료된 MFA 토큰 - 400 Bad Request")
    @WithMockUser
    void verifyMfa_expiredToken_badRequest() throws Exception {
        AuthDto.MfaVerifyRequest request = new AuthDto.MfaVerifyRequest("expired-token", "123456");

        given(authService.verifyMfa(any()))
            .willThrow(new IllegalArgumentException("MFA 세션이 만료되었거나 유효하지 않습니다."));

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── 로그아웃 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그아웃 - 200 OK 및 토큰 블랙리스트 등록")
    @WithMockUser
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(csrf())
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(eq("valid-token"));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 로그아웃 - 200 OK")
    @WithMockUser
    void logout_noHeader_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(null);
    }

    // ── 비밀번호 찾기 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("비밀번호 재설정 링크 요청 - 200 OK (이메일 미존재도 동일 응답)")
    @WithMockUser
    void requestPasswordReset_success() throws Exception {
        AuthDto.PasswordResetRequest request = new AuthDto.PasswordResetRequest("user@test.com");

        mockMvc.perform(post("/api/v1/auth/password-reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    // ── 아이디 찾기 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("아이디 찾기 성공 - 200 OK + 마스킹된 이메일")
    @WithMockUser
    void findEmail_success() throws Exception {
        AuthDto.FindEmailRequest request = new AuthDto.FindEmailRequest("홍길동", "010-1234-5678");
        AuthDto.FindEmailResponse response = new AuthDto.FindEmailResponse("us**@test.com");

        given(authService.findEmail(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/auth/find-email")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.maskedEmail").value("us**@test.com"));
    }
}
