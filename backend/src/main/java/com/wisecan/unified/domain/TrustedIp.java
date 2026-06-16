package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신뢰 IP — 등록된 IP에서 로그인할 때 2차 인증을 자동으로 패스한다.
 * 회원 한 명당 최대 10개까지 등록 가능 (운영 정책).
 */
@Entity
@Table(name = "trusted_ip",
       indexes = @Index(name = "idx_trusted_ip_member", columnList = "member_id"))
@Getter
@NoArgsConstructor
public class TrustedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** IPv4 또는 IPv6 주소 */
    @Column(nullable = false, length = 45)
    private String ipAddress;

    /** 사용자 지정 레이블 (예: "회사 사무실") */
    @Column(length = 100)
    private String label;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public TrustedIp(Long memberId, String ipAddress, String label) {
        this.memberId = memberId;
        this.ipAddress = ipAddress;
        this.label = label;
    }
}
