package com.wisecan.unified.service;

import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.TermCode;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.exception.AccountLockedException;
import com.wisecan.unified.exception.DuplicateEmailException;
import com.wisecan.unified.exception.TwoFactorRequiredException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.TermAgreementRepository;
import com.wisecan.unified.repository.TermCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private TermCodeRepository termCodeRepository;
    @Mock private TermAgreementRepository termAgreementRepository;
    @Mock private LoginAttemptService loginAttemptService;
    @Mock private TwoFactorAuthService twoFactorAuthService;
    @Mock private TrustedIpService trustedIpService;

    @InjectMocks
    private AuthService authService;

    // ── fixtures ────────────────────────────────────────────────────────────

    private List<AuthDto.TermAgreementItem> sampleTermAgreements() {
        return List.of(
            new AuthDto.TermAgreementItem("TOS", true),
            new AuthDto.TermAgreementItem("PRIVACY", true),
            new AuthDto.TermAgreementItem("MARKETING", false)
        );
    }

    private TermCode sampleTermCode(String code, String required) {
        return TermCode.builder()
            .code(code)
            .title(code + " 약관")
            .currentVersion("v1")
            .required(required)
            .status("ACTIVE")
            .build();
    }

    private Member normalMember() {
        return Member.builder()
            .email("user@test.com")
            .password("encodedPassword")
            .name("홍길동")
            .phone("010-1234-5678")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    // ── 회원가입 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("개인 회원가입 성공 — 약관 동의 포함")
    void register_success_withTermAgreements() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "user@test.com", "password123", "홍길동", "010-1234-5678",
            sampleTermAgreements()
        );
        Member saved = normalMember();

        given(memberRepository.existsByEmail("user@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class))).willReturn(saved);
        given(termCodeRepository.findByCode("TOS")).willReturn(Optional.of(sampleTermCode("TOS", "REQUIRED")));
        given(termCodeRepository.findByCode("PRIVACY")).willReturn(Optional.of(sampleTermCode("PRIVACY", "REQUIRED")));
        given(termCodeRepository.findByCode("MARKETING")).willReturn(Optional.of(sampleTermCode("MARKETING", "OPTIONAL")));
        given(jwtProvider.generateAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refreshToken");

        AuthDto.TokenResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.role()).isEqualTo("MEMBER");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 시 DuplicateEmailException 발생")
    void register_duplicateEmail_throwsException() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "dup@test.com", "password123", "홍길동", null,
            sampleTermAgreements()
        );
        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("dup@test.com");
    }

    @Test
    @DisplayName("필수 약관 미동의 시 IllegalArgumentException 발생")
    void register_requiredTermNotAgreed_throwsException() {
        List<AuthDto.TermAgreementItem> agreements = List.of(
            new AuthDto.TermAgreementItem("TOS", false),
            new AuthDto.TermAgreementItem("PRIVACY", true)
        );
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "user@test.com", "password123", "홍길동", null, agreements
        );

        given(memberRepository.existsByEmail("user@test.com")).willReturn(false);
        given(termCodeRepository.findByCode("TOS")).willReturn(Optional.of(sampleTermCode("TOS", "REQUIRED")));
        given(termCodeRepository.findByCode("PRIVACY")).willReturn(Optional.of(sampleTermCode("PRIVACY", "REQUIRED")));

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("필수 약관");
    }

    @Test
    @DisplayName("회원가입 시 비밀번호가 BCrypt로 해싱됨")
    void register_passwordHashed() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
            "user@test.com", "rawPassword", "홍길동", null,
            sampleTermAgreements()
        );
        Member saved = normalMember();

        given(memberRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode("rawPassword")).willReturn("$2a$hashedPassword");
        given(memberRepository.save(any(Member.class))).willReturn(saved);
        given(termCodeRepository.findByCode(anyString())).willReturn(Optional.of(sampleTermCode("TOS", "OPTIONAL")));
        given(jwtProvider.generateAccessToken(anyString(), anyString())).willReturn("token");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refresh");

        authService.register(request);

        verify(passwordEncoder).encode("rawPassword");
    }

    // ── 로그인 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 — 2차 인증 비활성 상태")
    void login_success_noMfa() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");
        Member member = normalMember();

        given(loginAttemptService.isLocked("user@test.com")).willReturn(false);
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refreshToken");

        AuthDto.TokenResponse response = authService.login(request, "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.email()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 RuntimeException 발생")
    void login_wrongPassword_throwsException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "wrongPassword");
        Member member = normalMember();

        given(loginAttemptService.isLocked("user@test.com")).willReturn(false);
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);
        given(loginAttemptService.recordFailure("user@test.com")).willReturn(1);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이메일 또는 비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 RuntimeException 발생")
    void login_emailNotFound_throwsException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("none@test.com", "password123");

        given(loginAttemptService.isLocked("none@test.com")).willReturn(false);
        given(memberRepository.findByEmail("none@test.com")).willReturn(Optional.empty());
        given(loginAttemptService.recordFailure("none@test.com")).willReturn(1);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이메일 또는 비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("Redis 잠금 상태에서 로그인 시 AccountLockedException 발생")
    void login_redisLocked_throwsAccountLockedException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");

        given(loginAttemptService.isLocked("user@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(AccountLockedException.class)
            .hasMessageContaining("15분");
    }

    @Test
    @DisplayName("비밀번호 5회 실패 시 AccountLockedException 발생")
    void login_fiveFailures_throwsAccountLockedException() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "wrongPw");
        Member member = normalMember();

        given(loginAttemptService.isLocked("user@test.com")).willReturn(false);
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);
        // 5번째 실패 시 카운터가 5 반환
        given(loginAttemptService.recordFailure("user@test.com")).willReturn(5);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
            .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("2차 인증 활성 + 비신뢰 IP → TwoFactorRequiredException 발생")
    void login_mfaEnabled_untrustedIp_throwsTwoFactorRequired() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");

        // 2차 인증이 활성화된 회원 생성
        Member member = Member.builder()
            .email("user@test.com")
            .password("encodedPassword")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();
        member.enableTwoFactor("EMAIL", null);

        given(loginAttemptService.isLocked("user@test.com")).willReturn(false);
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(trustedIpService.isTrusted(anyLong(), anyString())).willReturn(false);
        given(twoFactorAuthService.issueMfaSession("user@test.com", "EMAIL")).willReturn("mfa-token-xyz");

        assertThatThrownBy(() -> authService.login(request, "1.2.3.4"))
            .isInstanceOf(TwoFactorRequiredException.class)
            .satisfies(ex -> {
                TwoFactorRequiredException mfaEx = (TwoFactorRequiredException) ex;
                assertThat(mfaEx.getMfaToken()).isEqualTo("mfa-token-xyz");
            });
    }

    @Test
    @DisplayName("2차 인증 활성 + 신뢰 IP → 2차 인증 패스하고 토큰 즉시 발급")
    void login_mfaEnabled_trustedIp_skipsMfa() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user@test.com", "password123");

        Member member = Member.builder()
            .email("user@test.com")
            .password("encodedPassword")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();
        member.enableTwoFactor("EMAIL", null);

        given(loginAttemptService.isLocked("user@test.com")).willReturn(false);
        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(trustedIpService.isTrusted(anyLong(), anyString())).willReturn(true);
        given(jwtProvider.generateAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(anyString())).willReturn("refreshToken");

        AuthDto.TokenResponse response = authService.login(request, "192.168.1.100");

        assertThat(response.accessToken()).isEqualTo("accessToken");
    }

    // ── 아이디 찾기 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("이름+휴대폰 일치 시 마스킹된 이메일 반환")
    void findEmail_success_returnsMasked() {
        AuthDto.FindEmailRequest request = new AuthDto.FindEmailRequest("홍길동", "010-1234-5678");
        Member member = normalMember();

        given(memberRepository.findByNameAndPhone("홍길동", "010-1234-5678"))
            .willReturn(Optional.of(member));

        AuthDto.FindEmailResponse response = authService.findEmail(request);

        // "user@test.com" → "us**@test.com"
        assertThat(response.maskedEmail()).startsWith("us");
        assertThat(response.maskedEmail()).contains("@test.com");
        assertThat(response.maskedEmail()).contains("*");
    }

    @Test
    @DisplayName("이름+휴대폰 불일치 시 RuntimeException 발생")
    void findEmail_notFound_throwsException() {
        AuthDto.FindEmailRequest request = new AuthDto.FindEmailRequest("없는사람", "010-0000-0000");

        given(memberRepository.findByNameAndPhone("없는사람", "010-0000-0000"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findEmail(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("일치하는 회원 정보가 없습니다");
    }
}
