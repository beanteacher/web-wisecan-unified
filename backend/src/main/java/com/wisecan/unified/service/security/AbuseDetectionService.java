package com.wisecan.unified.service.security;

import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.security.AbuseBlock;
import com.wisecan.unified.domain.security.AbuseRuleType;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.security.AbuseBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보안·스팸·이상 패턴 자동 차단 서비스.
 *
 * 탐지 게이트(BurstVolumeGate, PatternRepeatGate, AnomalousHourGate)가 임계값을 초과할 때
 * 이 서비스를 호출하여 차단 기록을 생성하고 회원 상태를 SUSPENDED 로 전환한다.
 *
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-004~007 참조.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbuseDetectionService {

    private final AbuseBlockRepository abuseBlockRepository;
    private final MemberRepository memberRepository;

    /**
     * 이상 패턴 기록 및 자동 차단.
     *
     * 이미 동일 규칙으로 활성 차단 중인 경우 중복 기록하지 않는다.
     * 차단 기록 생성 후 회원 상태를 SUSPENDED 로 전환한다.
     *
     * @param memberId       차단 대상 회원 ID
     * @param apiKeyId       트리거 API Key ID (null 가능)
     * @param ruleType       탐지 규칙 유형
     * @param reason         상세 사유
     * @param measuredValue  측정값
     * @param thresholdValue 임계값
     */
    @Transactional
    public void recordAndBlock(Long memberId, Long apiKeyId, AbuseRuleType ruleType,
                               String reason, Long measuredValue, Long thresholdValue) {
        // 중복 차단 방지 — 이미 동일 규칙 활성 차단 존재 시 스킵
        if (abuseBlockRepository.findActiveByMemberIdAndRuleType(memberId, ruleType).isPresent()) {
            log.info("[AbuseDetection] 이미 활성 차단 존재 — memberId={}, rule={}", memberId, ruleType);
            return;
        }

        AbuseBlock block = AbuseBlock.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .ruleType(ruleType)
                .reason(reason)
                .measuredValue(measuredValue)
                .thresholdValue(thresholdValue)
                .autoBlocked(true)
                .build();
        abuseBlockRepository.save(block);

        // 회원 상태 SUSPENDED 전환 (이미 SUSPENDED 이면 상태 변경 없이 로그만)
        memberRepository.findById(memberId).ifPresent(member -> {
            if (member.getStatus() == MemberStatus.SUSPENDED) {
                log.warn("[AbuseDetection] 이미 SUSPENDED 상태 — memberId={}, rule={}", memberId, ruleType);
            } else if (member.getStatus() == MemberStatus.ACTIVE) {
                member.suspend();
                log.warn("[AbuseDetection] 회원 자동 차단 SUSPENDED — memberId={}, rule={}, reason={}",
                        memberId, ruleType, reason);
            }
        });
    }

    /**
     * 회원의 활성 차단 존재 여부.
     * AccountAbuseBlockGate 에서 발송 전 사전 체크에 사용한다.
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long memberId) {
        return abuseBlockRepository.existsActiveByMemberId(memberId);
    }

    /**
     * 차단 해제 (운영자 수동).
     * 해제 시 회원 상태도 ACTIVE 로 복구한다.
     */
    @Transactional
    public void unblock(Long blockId, String reason) {
        AbuseBlock block = abuseBlockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException("차단 기록을 찾을 수 없습니다: " + blockId));
        block.unblock(reason);

        // 동일 회원에 대한 다른 활성 차단이 없으면 ACTIVE 복구
        boolean otherActive = abuseBlockRepository
                .findActiveByMemberId(block.getMemberId())
                .stream()
                .anyMatch(b -> !b.getId().equals(blockId) && b.isActive());

        if (!otherActive) {
            memberRepository.findById(block.getMemberId()).ifPresent(member -> {
                if (member.getStatus() == MemberStatus.SUSPENDED) {
                    member.unsuspend();
                    log.info("[AbuseDetection] 회원 차단 해제 ACTIVE 복구 — memberId={}, blockId={}", block.getMemberId(), blockId);
                }
            });
        }
    }
}
