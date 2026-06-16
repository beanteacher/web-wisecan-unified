package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시스템 설정 엔티티 — W-503
 *
 * <p>운영자가 웹 콘솔에서 관리하는 키-값 기반 시스템 설정.
 * 예: 일일 발송 한도 기본값, 수신거부 기간, 스팸 키워드 등.</p>
 */
@Entity
@Table(name = "system_setting")
@Getter
@NoArgsConstructor
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public SystemSetting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    /**
     * 설정값 갱신.
     *
     * @param newValue  새로운 설정값
     * @param adminId   변경한 운영자 ID
     */
    public void updateValue(String newValue, Long adminId) {
        this.settingValue = newValue;
        this.updatedBy = adminId;
        this.updatedAt = LocalDateTime.now();
    }
}
