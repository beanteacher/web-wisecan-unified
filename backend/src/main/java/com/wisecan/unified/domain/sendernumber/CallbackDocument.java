package com.wisecan.unified.domain.sendernumber;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발신번호 증빙 서류 — EMPLOYEE / CORP_REP 케이스에서 필수.
 *
 * doc_type 카탈로그:
 *   EMPLOYMENT       — 재직증명서
 *   TELCO_USAGE      — 통신서비스 이용 증명서
 *   CORP_LICENSE     — 법인 등록증
 *   OWNERSHIP        — 번호 소유 증빙 기타
 */
@Entity
@Table(name = "callback_document", indexes = {
    @Index(name = "idx_cbdoc_callback", columnList = "callback_id")
})
@Getter
@NoArgsConstructor
public class CallbackDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "callback_id", nullable = false)
    private Long callbackId;

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    /** 클라우드 저장 경로 */
    @Column(name = "cloud_path", length = 500)
    private String cloudPath;

    /** 로컬 이중 보관 경로 */
    @Column(name = "local_path", length = 500)
    private String localPath;

    /** OCR 추출 텍스트 (Full-Text 검색용) */
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    @Builder
    public CallbackDocument(Long callbackId, String docType,
                             String cloudPath, String localPath,
                             String ocrText, Long sizeBytes) {
        this.callbackId = callbackId;
        this.docType = docType;
        this.cloudPath = cloudPath;
        this.localPath = localPath;
        this.ocrText = ocrText;
        this.sizeBytes = sizeBytes;
    }
}
