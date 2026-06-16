package com.wisecan.unified.domain.ocr;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OCR 문서 텍스트 추출 결과 엔티티 (§12.2).
 *
 * 발신번호 등록 서류(CallbackDocument) 또는 사업자 전환 서류(BusinessDocument)에서
 * 추출된 텍스트를 보관한다. OCR 어댑터는 Stub → 실 연동 교체 가능하도록 분리.
 * 클라우드(DB) + 로컬 파일 이중 저장 원칙 준수.
 */
@Entity
@Table(name = "ocr_document", indexes = {
    @Index(name = "idx_ocr_document_source", columnList = "source_type, source_id"),
    @Index(name = "idx_ocr_document_status", columnList = "status")
})
@Getter
@NoArgsConstructor
public class OcrDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 문서 출처 유형 (예: "CALLBACK_DOCUMENT", "BUSINESS_DOCUMENT")
     */
    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    /** 원본 문서 ID (CallbackDocument.id 또는 BusinessDocument.id) */
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** 원본 파일 URL (클라우드 저장 경로) */
    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    /** OCR 추출 텍스트 전문 */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OcrDocumentStatus status;

    /** 추출 실패 사유 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 로컬 백업 파일 경로 (이중 보관) */
    @Column(name = "local_backup_path", length = 1000)
    private String localBackupPath;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OcrDocumentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public OcrDocument(String sourceType, Long sourceId, String fileUrl) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.fileUrl = fileUrl;
        this.status = OcrDocumentStatus.PENDING;
    }

    /** OCR 추출 성공 처리 */
    public void markExtracted(String extractedText, String localBackupPath) {
        this.extractedText = extractedText;
        this.localBackupPath = localBackupPath;
        this.status = OcrDocumentStatus.EXTRACTED;
        this.extractedAt = LocalDateTime.now();
    }

    /** OCR 추출 실패 처리 */
    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = OcrDocumentStatus.FAILED;
    }

    /** 텍스트에 키워드가 포함되어 있는지 검색 */
    public boolean containsKeyword(String keyword) {
        if (this.extractedText == null || keyword == null) return false;
        return this.extractedText.toLowerCase().contains(keyword.toLowerCase());
    }
}
