package com.wisecan.unified.domain.security;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보안·스팸·이상 패턴 자동 차단 기록.
 * 탐지 규칙이 발동되면 이 엔티티에 기록하고, 회원 상태를 SUSPENDED 로 전환한다.
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-004~007 참조.
 */
@Entity
@Table(name = "abuse_block", indexes = {
        @Index(name = "idx_abuse_block_member", columnList = "member_id"),
        @Index(name = "idx_abuse_block_api_key", columnList = "api_key_id"),
        @Index(name = "idx_abuse_block_created", columnList = "created_at")
})
@Getter
@NoArgsConstructor
public class AbuseBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차단 대상 회원 ID */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 차단 트리거가 된 API Key ID (null 가능 — 계정 레벨 차단 시) */
    @Column(name = "api_key_id")
    private Long apiKeyId;

    /** 탐지 규칙 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private AbuseRuleType ruleType;

    /** 차단 상세 사유 */
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    /** 탐지 시점의 측정값 (예: 분당 발송 건수, 반복 횟수) */
    @Column(name = "measured_value")
    private Long measuredValue;

    /** 임계값 */
    @Column(name = "threshold_value")
    private Long thresholdValue;

    /** 자동 차단 여부 (false = 운영자 수동 차단) */
    @Column(name = "auto_blocked", nullable = false)
    private boolean autoBlocked;

    /** 차단 해제 시각 (null = 미해제) */
    @Column(name = "unblocked_at")
    private LocalDateTime unblockedAt;

    /** 차단 해제 사유 */
    @Column(name = "unblock_reason", length = 500)
    private String unblockReason;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public AbuseBlock(Long memberId, Long apiKeyId, AbuseRuleType ruleType,
                      String reason, Long measuredValue, Long thresholdValue,
                      boolean autoBlocked) {
        this.memberId = memberId;
        this.apiKeyId = apiKeyId;
        this.ruleType = ruleType;
        this.reason = reason;
        this.measuredValue = measuredValue;
        this.thresholdValue = thresholdValue;
        this.autoBlocked = autoBlocked;
    }

    /** 운영자가 차단을 해제한다 */
    public void unblock(String reason) {
        this.unblockedAt = LocalDateTime.now();
        this.unblockReason = reason;
    }

    /** 현재 활성 차단인지 여부 */
    public boolean isActive() {
        return this.unblockedAt == null;
    }
}
