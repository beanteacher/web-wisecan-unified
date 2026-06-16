package com.wisecan.unified.domain.cs;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 공지사항 엔티티.
 */
@Entity
@Table(name = "cs_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공지 유형: GENERAL(일반), MAINTENANCE(점검), IMPORTANT(중요) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 상단 고정 여부 */
    @Column(nullable = false)
    private boolean pinned;

    /** 노출 여부 */
    @Column(nullable = false)
    private boolean visible;

    /** 작성자 운영자 ID */
    @Column(nullable = false)
    private Long authorAdminId;

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
    public Notice(NoticeType type, String title, String content,
                  boolean pinned, boolean visible, Long authorAdminId) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.visible = visible;
        this.authorAdminId = authorAdminId;
    }

    public void update(NoticeType type, String title, String content,
                       boolean pinned, boolean visible) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.visible = visible;
    }
}
