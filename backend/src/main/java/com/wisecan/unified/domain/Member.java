package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column
    private Long companyId;

    @Column
    private LocalDateTime emailVerifiedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @Column
    private LocalDateTime withdrawnAt;

    // 2차 인증 활성 여부
    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    // 2차 인증 수단: EMAIL | TOTP
    @Column(length = 10)
    private String twoFactorMethod;

    // TOTP 시크릿 (AES 암호화 저장)
    @Column(length = 500)
    private String totpSecret;

    // 로그인 연속 실패 횟수 (Redis 관리가 원칙이나 백업 필드)
    @Column(nullable = false)
    private int loginFailCount = 0;

    // 계정 잠금 해제 시각
    @Column
    private LocalDateTime lockedUntil;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Member(String email, String password, String name, String phone,
                  MemberRole role, MemberStatus status, Long companyId) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.companyId = companyId;
    }

    public void promoteToCompanyMaster(Long companyId) {
        this.role = MemberRole.COMPANY_MASTER;
        this.companyId = companyId;
    }

    public void demoteToMember() {
        this.role = MemberRole.MEMBER;
        this.updatedAt = LocalDateTime.now();
    }

    public void clearCompany() {
        this.companyId = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /** 로그인 성공 시 실패 카운터 초기화 */
    public void resetLoginFail() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

    /** 로그인 실패 시 카운터 증가, 5회 달성 시 15분 잠금 */
    public void recordLoginFailure() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    /** 계정이 현재 잠금 상태인지 여부 */
    public boolean isLocked() {
        return this.lockedUntil != null && LocalDateTime.now().isBefore(this.lockedUntil);
    }

    /** 2차 인증 활성화 (이메일 또는 TOTP) */
    public void enableTwoFactor(String method, String totpSecret) {
        this.twoFactorEnabled = true;
        this.twoFactorMethod = method;
        this.totpSecret = totpSecret;
    }

    /** 2차 인증 비활성화 */
    public void disableTwoFactor() {
        this.twoFactorEnabled = false;
        this.twoFactorMethod = null;
        this.totpSecret = null;
    }
}
