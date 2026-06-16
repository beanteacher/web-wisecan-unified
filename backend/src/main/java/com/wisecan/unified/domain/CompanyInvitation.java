package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_invitation")
@Getter
@NoArgsConstructor
public class CompanyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long inviterMemberId;

    @Column(nullable = false, length = 255)
    private String inviteeEmail;

    @Column(length = 20)
    private String inviteePhone;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(updatable = false)
    private LocalDateTime invitedAt;

    @Column
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime acceptedAt;

    @PrePersist
    protected void onCreate() {
        this.invitedAt = LocalDateTime.now();
    }

    @Builder
    public CompanyInvitation(Long companyId, Long inviterMemberId, String inviteeEmail,
                              String inviteePhone, String tokenHash, LocalDateTime expiresAt) {
        this.companyId = companyId;
        this.inviterMemberId = inviterMemberId;
        this.inviteeEmail = inviteeEmail;
        this.inviteePhone = inviteePhone;
        this.status = "PENDING";
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public void accept() {
        this.status = "ACCEPTED";
        this.acceptedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = "EXPIRED";
    }

    public void revoke() {
        this.status = "REVOKED";
    }

    public boolean isUsable() {
        return "PENDING".equals(this.status)
                && this.expiresAt != null
                && this.expiresAt.isAfter(LocalDateTime.now());
    }
}
