package com.wisecan.unified.domain.ocr;

/**
 * OCR 어댑터 인터페이스 — 실 OCR 엔진(클로바, Tesseract 등)으로 교체 가능한 포트.
 * MVP 단계에서는 StubOcrAdapter를 사용하며, 실 연동 시 구현체를 교체한다.
 */
public interface OcrAdapter {

    /**
     * 파일 URL에서 텍스트를 추출한다.
     *
     * @param fileUrl 원본 파일 URL (클라우드 저장 경로)
     * @return 추출된 텍스트 (실패 시 예외 발생)
     */
    String extractText(String fileUrl);
}
