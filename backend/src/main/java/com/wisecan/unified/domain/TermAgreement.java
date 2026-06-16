package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "term_agreement",
       uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "term_code_id"}))
@Getter
@NoArgsConstructor
public class TermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long termCodeId;

    @Column(nullable = false, length = 20)
    private String agreedVersion;

    @Column(nullable = false, length = 20)
    private String agreement;

    @Column
    private LocalDateTime agreedAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.agreedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public TermAgreement(Long memberId, Long termCodeId, String agreedVersion, String agreement) {
        this.memberId = memberId;
        this.termCodeId = termCodeId;
        this.agreedVersion = agreedVersion;
        this.agreement = agreement;
    }
}
