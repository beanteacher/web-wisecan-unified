package com.wisecan.unified.domain.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OCR Stub 어댑터 — 실 OCR 엔진 미연동 시 사용하는 개발/MVP 전용 구현체.
 * application.yml 에서 ocr.stub=true (기본값) 이면 활성화된다.
 * 실 OCR 연동 시 이 클래스를 비활성화하고 실 구현체를 등록한다.
 */
@Component
@ConditionalOnProperty(name = "ocr.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubOcrAdapter implements OcrAdapter {

    @Override
    public String extractText(String fileUrl) {
        log.info("[OCR-STUB] 텍스트 추출 요청: {}", fileUrl);
        // Stub: 파일 URL 기반 결정적 더미 텍스트 반환
        return "[OCR-STUB] 추출된 텍스트 샘플 — 사업자등록번호: 123-45-67890, " +
               "상호명: 주식회사 위즈캔, 대표자: 홍길동, 소재지: 서울특별시 강남구. " +
               "fileUrl=" + fileUrl;
    }
}
