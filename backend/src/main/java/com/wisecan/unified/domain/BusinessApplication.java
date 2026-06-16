package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_application")
@Getter
@NoArgsConstructor
public class BusinessApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 12)
    private String bizNumber;

    @Column(length = 13)
    private String corpNumber;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 100)
    private String ceoName;

    @Column(length = 20)
    private String ceoPhone;

    @Column(length = 500)
    private String rejectReason;

    @Column
    private Long reviewedBy;

    @Column(updatable = false)
    private LocalDateTime submittedAt;

    @Column
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }

    @Builder
    public BusinessApplication(Long memberId, String status, String bizNumber,
                                String corpNumber, String companyName,
                                String ceoName, String ceoPhone) {
        this.memberId = memberId;
        this.status = status;
        this.bizNumber = bizNumber;
        this.corpNumber = corpNumber;
        this.companyName = companyName;
        this.ceoName = ceoName;
        this.ceoPhone = ceoPhone;
    }

    public void approve(Long reviewedBy) {
        this.status = "APPROVED";
        this.reviewedBy = reviewedBy;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(Long reviewedBy, String reason) {
        this.status = "REJECTED";
        this.reviewedBy = reviewedBy;
        this.rejectReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    public void startReview() {
        this.status = "UNDER_REVIEW";
    }
}
