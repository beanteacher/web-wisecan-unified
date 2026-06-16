package com.wisecan.unified.domain.trial;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체험 세션 엔티티 (W-406).
 *
 * <p>비회원 체험 모드 진입 시 발급되는 격리된 세션이다.
 * 운영 DB {@code member}, {@code send_request} 등과 무관하게 별도 테이블에 적재한다.</p>
 *
 * <p>만료 정책: 생성 시각 기준 30분. 만료된 세션은 체험 데이터와 함께 폐기 대상이 된다.</p>
 */
@Entity
@Table(name = "trial_session")
@Getter
@NoArgsConstructor
public class TrialSession {

    /** 체험 세션 토큰 (UUID) — Primary Key */
    @Id
    @Column(name = "session_token", length = 36, nullable = false)
    private String sessionToken;

    /** 요청 IP (어뷰징 차단 임계치 판별용) */
    @Column(name = "client_ip", length = 64, nullable = false)
    private String clientIp;

    /** 세션 만료 일시 (기본 30분) */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 세션 생성 일시 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /** 체험 세션 종료(만료·사용자 종료) 일시 — null 이면 활성 */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public TrialSession(String sessionToken, String clientIp, LocalDateTime expiresAt) {
        this.sessionToken = sessionToken;
        this.clientIp = clientIp;
        this.expiresAt = expiresAt;
    }

    /** 세션이 현재 유효한지 확인한다. */
    public boolean isActive() {
        return closedAt == null && LocalDateTime.now().isBefore(expiresAt);
    }

    /** 세션을 명시적으로 종료한다. */
    public void close() {
        if (this.closedAt == null) {
            this.closedAt = LocalDateTime.now();
        }
    }
}
