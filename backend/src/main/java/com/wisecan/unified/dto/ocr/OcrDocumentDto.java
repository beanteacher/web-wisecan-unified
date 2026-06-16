package com.wisecan.unified.dto.ocr;

import com.wisecan.unified.domain.ocr.OcrDocument;
import com.wisecan.unified.domain.ocr.OcrDocumentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class OcrDocumentDto {

    /** OCR 추출 요청 */
    public record ExtractRequest(
            @NotBlank String sourceType,
            @NotNull Long sourceId,
            @NotBlank String fileUrl
    ) {}

    public record Summary(
            Long id,
            String sourceType,
            Long sourceId,
            String fileUrl,
            OcrDocumentStatus status,
            LocalDateTime extractedAt,
            LocalDateTime createdAt
    ) {
        public static Summary from(OcrDocument doc) {
            return new Summary(
                    doc.getId(),
                    doc.getSourceType(),
                    doc.getSourceId(),
                    doc.getFileUrl(),
                    doc.getStatus(),
                    doc.getExtractedAt(),
                    doc.getCreatedAt()
            );
        }
    }

    public record Detail(
            Long id,
            String sourceType,
            Long sourceId,
            String fileUrl,
            OcrDocumentStatus status,
            String extractedText,
            String localBackupPath,
            String errorMessage,
            LocalDateTime extractedAt,
            LocalDateTime createdAt
    ) {
        public static Detail from(OcrDocument doc) {
            return new Detail(
                    doc.getId(),
                    doc.getSourceType(),
                    doc.getSourceId(),
                    doc.getFileUrl(),
                    doc.getStatus(),
                    doc.getExtractedText(),
                    doc.getLocalBackupPath(),
                    doc.getErrorMessage(),
                    doc.getExtractedAt(),
                    doc.getCreatedAt()
            );
        }
    }

    /** 키워드 검색 결과 항목 */
    public record SearchResult(
            Long id,
            String sourceType,
            Long sourceId,
            String fileUrl,
            String snippet,
            LocalDateTime extractedAt
    ) {
        public static SearchResult from(OcrDocument doc, String keyword) {
            String snippet = extractSnippet(doc.getExtractedText(), keyword);
            return new SearchResult(
                    doc.getId(),
                    doc.getSourceType(),
                    doc.getSourceId(),
                    doc.getFileUrl(),
                    snippet,
                    doc.getExtractedAt()
            );
        }

        private static String extractSnippet(String text, String keyword) {
            if (text == null || keyword == null) return "";
            int idx = text.toLowerCase().indexOf(keyword.toLowerCase());
            if (idx < 0) return text.substring(0, Math.min(100, text.length()));
            int start = Math.max(0, idx - 40);
            int end = Math.min(text.length(), idx + keyword.length() + 60);
            return (start > 0 ? "..." : "") + text.substring(start, end) + (end < text.length() ? "..." : "");
        }
    }
}
