package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_role_log")
@Getter
@NoArgsConstructor
public class CompanyRoleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column
    private Long fromMemberId;

    @Column
    private Long toMemberId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(nullable = false)
    private Long actorMemberId;

    @Column(updatable = false)
    private LocalDateTime actedAt;

    @PrePersist
    protected void onCreate() {
        this.actedAt = LocalDateTime.now();
    }

    @Builder
    public CompanyRoleLog(Long companyId, Long fromMemberId, Long toMemberId,
                           String action, String reason, Long actorMemberId) {
        this.companyId = companyId;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.action = action;
        this.reason = reason;
        this.actorMemberId = actorMemberId;
    }
}
