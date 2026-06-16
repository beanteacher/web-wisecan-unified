package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "term_code")
@Getter
@NoArgsConstructor
public class TermCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    private String currentVersion;

    @Column(nullable = false, length = 20)
    private String required;

    @Column
    private LocalDate effectiveDate;

    @Column
    private LocalDate notifiedDate;

    @Column(length = 500)
    private String fileCloudPath;

    @Column(length = 500)
    private String fileLocalPath;

    @Column(nullable = false, length = 20)
    private String status;

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
    public TermCode(String code, String title, String currentVersion, String required,
                    LocalDate effectiveDate, LocalDate notifiedDate,
                    String fileCloudPath, String fileLocalPath, String status) {
        this.code = code;
        this.title = title;
        this.currentVersion = currentVersion;
        this.required = required;
        this.effectiveDate = effectiveDate;
        this.notifiedDate = notifiedDate;
        this.fileCloudPath = fileCloudPath;
        this.fileLocalPath = fileLocalPath;
        this.status = status;
    }
}
