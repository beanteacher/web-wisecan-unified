package com.wisecan.unified.domain.ocr;

/**
 * OCR 문서 처리 상태.
 */
public enum OcrDocumentStatus {
    /** 추출 대기 */
    PENDING,
    /** 추출 완료 */
    EXTRACTED,
    /** 추출 실패 */
    FAILED
}
