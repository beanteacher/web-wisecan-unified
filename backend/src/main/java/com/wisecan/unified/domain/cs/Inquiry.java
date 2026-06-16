package com.wisecan.unified.domain.cs;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 1:1 문의 엔티티.
 * createdAt → answeredAt 간격으로 24h SLA 측정.
 */
@Entity
@Table(name = "cs_inquiry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문의 작성자 회원 ID */
    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    /** 운영자 답변 내용 */
    @Column(columnDefinition = "TEXT")
    private String answerContent;

    /** 답변한 운영자 ID */
    @Column
    private Long answeredByAdminId;

    /** 답변 완료 시각 — SLA 측정 기준점 */
    @Column
    private LocalDateTime answeredAt;

    /** 접수 시각 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = InquiryStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Inquiry(Long memberId, InquiryCategory category, String title, String content) {
        this.memberId = memberId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.OPEN;
    }

    /** 운영자 답변 등록 — 상태를 ANSWERED 로 전환하고 SLA 시각 기록 */
    public void answer(String answerContent, Long adminId) {
        this.answerContent = answerContent;
        this.answeredByAdminId = adminId;
        this.answeredAt = LocalDateTime.now();
        this.status = InquiryStatus.ANSWERED;
    }

    /** 문의자 종료 처리 */
    public void close() {
        this.status = InquiryStatus.CLOSED;
    }

    /** 운영자 처리 중 상태 전환 */
    public void markInProgress() {
        if (this.status == InquiryStatus.OPEN) {
            this.status = InquiryStatus.IN_PROGRESS;
        }
    }

    /**
     * 24h SLA 충족 여부.
     * answeredAt 이 없으면 아직 미답변이므로 false.
     */
    public boolean isSlaBreached() {
        if (this.answeredAt == null) {
            return false;
        }
        return this.answeredAt.isAfter(this.createdAt.plusHours(24));
    }

    /**
     * 답변 소요 시간(분). 미답변 시 -1.
     */
    public long getAnswerMinutes() {
        if (this.answeredAt == null) {
            return -1L;
        }
        return java.time.Duration.between(this.createdAt, this.answeredAt).toMinutes();
    }
}
