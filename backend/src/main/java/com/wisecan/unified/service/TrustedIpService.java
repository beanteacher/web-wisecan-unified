package com.wisecan.unified.service;

import com.wisecan.unified.domain.TrustedIp;
import com.wisecan.unified.dto.AuthDto;
import com.wisecan.unified.repository.TrustedIpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 신뢰 IP 등록·판별 서비스 (스펙 §1.3 예외: "신뢰 IP 등록된 경우 2차 인증 자동 패스").
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrustedIpService {

    private static final int MAX_TRUSTED_IPS = 10;

    private final TrustedIpRepository trustedIpRepository;

    /** 요청 IP가 해당 회원의 신뢰 IP 목록에 있는지 확인 */
    @Transactional(readOnly = true)
    public boolean isTrusted(Long memberId, String ipAddress) {
        return trustedIpRepository.existsByMemberIdAndIpAddress(memberId, ipAddress);
    }

    /** 신뢰 IP 목록 조회 */
    @Transactional(readOnly = true)
    public List<AuthDto.TrustedIpItem> list(Long memberId) {
        return trustedIpRepository.findByMemberId(memberId).stream()
            .map(t -> new AuthDto.TrustedIpItem(t.getId(), t.getIpAddress(), t.getLabel(), t.getCreatedAt()))
            .toList();
    }

    /** 신뢰 IP 등록 */
    @Transactional
    public AuthDto.TrustedIpItem register(Long memberId, AuthDto.TrustedIpRegisterRequest request) {
        if (trustedIpRepository.countByMemberId(memberId) >= MAX_TRUSTED_IPS) {
            throw new IllegalStateException("신뢰 IP는 최대 " + MAX_TRUSTED_IPS + "개까지 등록할 수 있습니다.");
        }
        if (trustedIpRepository.existsByMemberIdAndIpAddress(memberId, request.ipAddress())) {
            throw new IllegalStateException("이미 등록된 IP 주소입니다.");
        }

        TrustedIp saved = trustedIpRepository.save(
            TrustedIp.builder()
                .memberId(memberId)
                .ipAddress(request.ipAddress())
                .label(request.label())
                .build()
        );

        log.info("신뢰 IP 등록 — memberId={}, ip={}", memberId, request.ipAddress());
        return new AuthDto.TrustedIpItem(saved.getId(), saved.getIpAddress(), saved.getLabel(), saved.getCreatedAt());
    }

    /** 신뢰 IP 삭제 */
    @Transactional
    public void delete(Long memberId, Long trustedIpId) {
        if (!trustedIpRepository.existsById(trustedIpId)) {
            throw new com.wisecan.unified.exception.EntityNotFoundException("신뢰 IP를 찾을 수 없습니다.");
        }
        trustedIpRepository.deleteByMemberIdAndId(memberId, trustedIpId);
        log.info("신뢰 IP 삭제 — memberId={}, trustedIpId={}", memberId, trustedIpId);
    }
}
