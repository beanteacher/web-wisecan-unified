package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_document")
@Getter
@NoArgsConstructor
public class BusinessDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false, length = 30)
    private String docType;

    @Column(length = 500)
    private String cloudPath;

    @Column(length = 500)
    private String localPath;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String ocrText;

    @Column(length = 64)
    private String checksum;

    @Column
    private Long sizeBytes;

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    @Builder
    public BusinessDocument(Long applicationId, String docType,
                             String cloudPath, String localPath,
                             String checksum, Long sizeBytes) {
        this.applicationId = applicationId;
        this.docType = docType;
        this.cloudPath = cloudPath;
        this.localPath = localPath;
        this.checksum = checksum;
        this.sizeBytes = sizeBytes;
    }
}
