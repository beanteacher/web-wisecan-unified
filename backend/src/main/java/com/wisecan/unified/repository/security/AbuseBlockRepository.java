package com.wisecan.unified.repository.security;

import com.wisecan.unified.domain.security.AbuseBlock;
import com.wisecan.unified.domain.security.AbuseRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보안·스팸·이상 패턴 차단 기록 리포지토리.
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-004~007 참조.
 */
public interface AbuseBlockRepository extends JpaRepository<AbuseBlock, Long> {

    /** 회원의 활성 차단 기록 조회 (미해제 건) */
    @Query("SELECT b FROM AbuseBlock b WHERE b.memberId = :memberId AND b.unblockedAt IS NULL")
    List<AbuseBlock> findActiveByMemberId(@Param("memberId") Long memberId);

    /** 특정 규칙 유형으로 활성 차단된 기록 단건 조회 */
    @Query("SELECT b FROM AbuseBlock b WHERE b.memberId = :memberId AND b.ruleType = :ruleType AND b.unblockedAt IS NULL")
    Optional<AbuseBlock> findActiveByMemberIdAndRuleType(
            @Param("memberId") Long memberId,
            @Param("ruleType") AbuseRuleType ruleType);

    /** API Key 기준 활성 차단 여부 */
    @Query("SELECT COUNT(b) > 0 FROM AbuseBlock b WHERE b.apiKeyId = :apiKeyId AND b.unblockedAt IS NULL")
    boolean existsActiveByApiKeyId(@Param("apiKeyId") Long apiKeyId);

    /** 회원의 활성 차단 존재 여부 */
    @Query("SELECT COUNT(b) > 0 FROM AbuseBlock b WHERE b.memberId = :memberId AND b.unblockedAt IS NULL")
    boolean existsActiveByMemberId(@Param("memberId") Long memberId);
}
