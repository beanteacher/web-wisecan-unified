package com.wisecan.unified.service;

import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.TermAgreement;
import com.wisecan.unified.domain.TermCode;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.exception.AccountLockedException;
import com.wisecan.unified.exception.DuplicateEmailException;
import com.wisecan.unified.exception.TwoFactorRequiredException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.TermAgreementRepository;
import com.wisecan.unified.repository.TermCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final TermCodeRepository termCodeRepository;
    private final TermAgreementRepository termAgreementRepository;
    private final LoginAttemptService loginAttemptService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final TrustedIpService trustedIpService;

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        // 약관 동의 검증 — 필수 약관은 반드시 동의해야 함
        validateRequiredTerms(request.termAgreements());

        Member member = Member.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .phone(request.phone())
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        Member savedMember = memberRepository.save(member);

        // 약관 동의 이력 저장
        saveTermAgreements(savedMember.getId(), request.termAgreements());

        String accessToken = jwtProvider.generateAccessToken(savedMember.getEmail(), savedMember.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(savedMember.getEmail());

        return new AuthDto.TokenResponse(
            accessToken, refreshToken,
            savedMember.getEmail(), savedMember.getName(),
            savedMember.getRole().name()
        );
    }

    /**
     * 로그인 (스펙 §1.3 RQ-AUTH-301~311).
     *
     * 흐름:
     *   1. 계정 잠금 여부 확인 (Redis 카운터 기준, 엔티티 필드 보조)
     *   2. 이메일/비밀번호 검증
     *   3. 실패 시 카운터 증가 → 5회 시 잠금 예외
     *   4. 성공 시: 신뢰 IP면 즉시 토큰 발급 / 2차 인증 활성이면 MFA 세션 발급
     *
     * @param request  이메일 + 비밀번호
     * @param clientIp 요청 IP (신뢰 IP 판별에 사용)
     * @return TokenResponse (2차 인증 불필요) 또는 TwoFactorRequiredException (2차 인증 필요)
     */
    @Transactional
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request, String clientIp) {
        // 1. Redis 기준 잠금 확인
        if (loginAttemptService.isLocked(request.email())) {
            throw new AccountLockedException();
        }

        // 2. 회원 조회
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                loginAttemptService.recordFailure(request.email());
                return new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
            });

        // 3. 엔티티 잠금 상태 재확인 (Redis 없는 환경 대비)
        if (member.isLocked()) {
            throw new AccountLockedException();
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            int failCount = loginAttemptService.recordFailure(request.email());
            member.recordLoginFailure();
            // memberRepository.save(member) — dirty checking으로 자동 반영

            if (failCount >= 5 || member.isLocked()) {
                throw new AccountLockedException();
            }
            throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 5. 로그인 성공 처리
        loginAttemptService.resetFailure(request.email());
        member.resetLoginFail();
        member.updateLastLogin();

        // 6. 2차 인증 판별
        if (member.isTwoFactorEnabled()) {
            // 신뢰 IP면 2차 인증 자동 패스
            boolean trusted = clientIp != null && trustedIpService.isTrusted(member.getId(), clientIp);
            if (!trusted) {
                String mfaToken = twoFactorAuthService.issueMfaSession(
                    member.getEmail(), member.getTwoFactorMethod()
                );
                throw new TwoFactorRequiredException(mfaToken);
            }
            log.debug("신뢰 IP({})로 2차 인증 자동 패스 — {}", clientIp, member.getEmail());
        }

        return buildTokenResponse(member);
    }

    /**
     * 2차 인증 코드 검증 후 최종 토큰 발급 (스펙 §1.3 RQ-AUTH-310~311).
     */
    @Transactional(readOnly = true)
    public AuthDto.TokenResponse verifyMfa(AuthDto.MfaVerifyRequest request) {
        String email = twoFactorAuthService.resolveEmail(request.mfaToken());
        if (email == null) {
            throw new IllegalArgumentException("MFA 세션이 만료되었거나 유효하지 않습니다.");
        }

        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        twoFactorAuthService.verifyMfaCode(
            request.mfaToken(), request.code(),
            member.getTwoFactorMethod(), member.getTotpSecret()
        );

        return buildTokenResponse(member);
    }

    /**
     * 비밀번호 재설정 링크 발송 (스펙 §1.4 RQ-AUTH-303).
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(AuthDto.PasswordResetRequest request) {
        // 이메일이 존재하지 않아도 동일 응답 (사용자 열거 방지)
        memberRepository.findByEmail(request.email()).ifPresent(member -> {
            // TODO: 재설정 토큰 생성 후 Redis 저장 + 메일 발송
            log.info("[PW RESET] 비밀번호 재설정 링크 발송 → {}", member.getEmail());
        });
    }

    /**
     * 비밀번호 재설정 실행 (스펙 §1.4 RQ-AUTH-304).
     */
    @Transactional
    public void confirmPasswordReset(AuthDto.PasswordResetConfirmRequest request) {
        // TODO: Redis에서 token → email 조회 후 비밀번호 업데이트
        // 현재는 인터페이스 확정 단계 — 토큰 검증 stub
        log.info("[PW RESET] 비밀번호 재설정 실행 — token={}", request.token());
        throw new UnsupportedOperationException("비밀번호 재설정 토큰 저장소 연동 필요");
    }

    /**
     * 아이디(이메일) 찾기 (스펙 §1.4 RQ-AUTH-303).
     * 이름 + 휴대폰 번호로 회원 조회 후 마스킹된 이메일 반환.
     */
    @Transactional(readOnly = true)
    public AuthDto.FindEmailResponse findEmail(AuthDto.FindEmailRequest request) {
        Member member = memberRepository.findByNameAndPhone(request.name(), request.phone())
            .orElseThrow(() -> new RuntimeException("일치하는 회원 정보가 없습니다."));
        return new AuthDto.FindEmailResponse(maskEmail(member.getEmail()));
    }

    public void logout(String token) {
        if (token == null || token.isBlank() || !jwtProvider.validateToken(token)) {
            return;
        }
        long ttl = jwtProvider.getExpiration(token).getTime() - System.currentTimeMillis();
        tokenBlacklistService.blacklist(token, ttl);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AuthDto.TokenResponse buildTokenResponse(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getEmail(), member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(member.getEmail());
        return new AuthDto.TokenResponse(
            accessToken, refreshToken,
            member.getEmail(), member.getName(),
            member.getRole().name()
        );
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        // 앞 2자리만 노출, 나머지 *
        String masked = local.substring(0, Math.min(2, local.length()))
            + "*".repeat(Math.max(0, local.length() - 2));
        return masked + domain;
    }

    private void validateRequiredTerms(List<AuthDto.TermAgreementItem> termAgreements) {
        if (termAgreements == null) {
            return;
        }
        for (AuthDto.TermAgreementItem item : termAgreements) {
            termCodeRepository.findByCode(item.termCode()).ifPresent(termCode -> {
                if ("REQUIRED".equals(termCode.getRequired()) && !item.agreed()) {
                    throw new IllegalArgumentException(
                        "필수 약관 '" + termCode.getTitle() + "'에 동의해야 합니다."
                    );
                }
            });
        }
    }

    private void saveTermAgreements(Long memberId, List<AuthDto.TermAgreementItem> termAgreements) {
        if (termAgreements == null) {
            return;
        }
        for (AuthDto.TermAgreementItem item : termAgreements) {
            termCodeRepository.findByCode(item.termCode()).ifPresent(termCode -> {
                TermAgreement agreement = TermAgreement.builder()
                    .memberId(memberId)
                    .termCodeId(termCode.getId())
                    .agreedVersion(termCode.getCurrentVersion())
                    .agreement(item.agreed() ? "AGREED" : "WITHDRAWN")
                    .build();
                termAgreementRepository.save(agreement);
            });
        }
    }
}
