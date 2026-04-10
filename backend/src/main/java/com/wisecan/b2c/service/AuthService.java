package com.wisecan.b2c.service;

import com.wisecan.b2c.config.JwtProvider;
import com.wisecan.b2c.domain.Member;
import com.wisecan.b2c.domain.MemberRole;
import com.wisecan.b2c.domain.MemberStatus;
import com.wisecan.b2c.dto.AuthDto;
import com.wisecan.b2c.exception.DuplicateEmailException;
import com.wisecan.b2c.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Member member = Member.builder()
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .name(request.name())
            .role(MemberRole.USER)
            .status(MemberStatus.ACTIVE)
            .build();

        memberRepository.save(member);

        String accessToken = jwtProvider.generateAccessToken(member.getEmail(), member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(member.getEmail());

        return new AuthDto.TokenResponse(accessToken, refreshToken, member.getEmail(), member.getName());
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다"));
        log.info("passwor: {}",passwordEncoder.encode(request.password()));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다");
        }

        String accessToken = jwtProvider.generateAccessToken(member.getEmail(), member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(member.getEmail());

        return new AuthDto.TokenResponse(accessToken, refreshToken, member.getEmail(), member.getName());
    }
}
