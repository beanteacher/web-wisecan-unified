package com.wisecan.unified.domain.cs;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FAQ 항목. 챗봇 Stub 응답 소스로도 사용.
 */
@Entity
@Table(name = "cs_faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 300)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    /** 노출 순서 (작을수록 상위) */
    @Column(nullable = false)
    private int sortOrder;

    /** 노출 여부 */
    @Column(nullable = false)
    private boolean visible;

    @Column(nullable = false, updatable = false)
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
    public Faq(InquiryCategory category, String question, String answer, int sortOrder, boolean visible) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }

    public void update(String question, String answer, InquiryCategory category, int sortOrder, boolean visible) {
        this.question = question;
        this.answer = answer;
        this.category = category;
        this.sortOrder = sortOrder;
        this.visible = visible;
    }
}
