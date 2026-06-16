package com.wisecan.unified.service.ocr;

import com.wisecan.unified.domain.ocr.OcrAdapter;
import com.wisecan.unified.domain.ocr.OcrDocument;
import com.wisecan.unified.domain.ocr.OcrDocumentStatus;
import com.wisecan.unified.dto.ocr.OcrDocumentDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ocr.OcrDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock private OcrDocumentRepository ocrDocumentRepository;
    @Mock private OcrAdapter ocrAdapter;

    @InjectMocks
    private OcrService ocrService;

    // ── extract() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("OCR 추출 — 신규 문서: 어댑터 호출 후 EXTRACTED 상태로 저장")
    void extract_newDocument_callsAdapterAndSavesExtracted() {
        OcrDocumentDto.ExtractRequest request = new OcrDocumentDto.ExtractRequest(
                "CALLBACK_DOCUMENT", 10L, "https://storage.example.com/doc.pdf");

        given(ocrDocumentRepository.findBySourceTypeAndSourceId("CALLBACK_DOCUMENT", 10L))
                .willReturn(Optional.empty());

        OcrDocument savedDoc = OcrDocument.builder()
                .sourceType("CALLBACK_DOCUMENT")
                .sourceId(10L)
                .fileUrl("https://storage.example.com/doc.pdf")
                .build();
        given(ocrDocumentRepository.save(any(OcrDocument.class))).willReturn(savedDoc);
        given(ocrAdapter.extractText(anyString()))
                .willReturn("사업자등록번호: 123-45-67890 상호명: 주식회사 테스트");

        OcrDocumentDto.Detail result = ocrService.extract(request);

        verify(ocrAdapter).extractText("https://storage.example.com/doc.pdf");
        verify(ocrDocumentRepository).findBySourceTypeAndSourceId("CALLBACK_DOCUMENT", 10L);
    }

    @Test
    @DisplayName("OCR 추출 — 이미 추출된 문서: 어댑터 재호출 없이 기존 결과 반환")
    void extract_alreadyExtracted_returnsCachedResult() {
        OcrDocumentDto.ExtractRequest request = new OcrDocumentDto.ExtractRequest(
                "CALLBACK_DOCUMENT", 10L, "https://storage.example.com/doc.pdf");

        OcrDocument existing = OcrDocument.builder()
                .sourceType("CALLBACK_DOCUMENT")
                .sourceId(10L)
                .fileUrl("https://storage.example.com/doc.pdf")
                .build();
        existing.markExtracted("기존 추출 텍스트", "/local/backup/ocr_10.txt");

        given(ocrDocumentRepository.findBySourceTypeAndSourceId("CALLBACK_DOCUMENT", 10L))
                .willReturn(Optional.of(existing));

        OcrDocumentDto.Detail result = ocrService.extract(request);

        verify(ocrAdapter, never()).extractText(anyString());
        assertThat(result.status()).isEqualTo(OcrDocumentStatus.EXTRACTED);
        assertThat(result.extractedText()).isEqualTo("기존 추출 텍스트");
    }

    @Test
    @DisplayName("OCR 추출 — 어댑터 예외 발생 시 FAILED 상태로 저장")
    void extract_adapterFails_marksAsFailed() {
        OcrDocumentDto.ExtractRequest request = new OcrDocumentDto.ExtractRequest(
                "BUSINESS_DOCUMENT", 5L, "https://storage.example.com/biz.pdf");

        given(ocrDocumentRepository.findBySourceTypeAndSourceId("BUSINESS_DOCUMENT", 5L))
                .willReturn(Optional.empty());

        OcrDocument savedDoc = OcrDocument.builder()
                .sourceType("BUSINESS_DOCUMENT")
                .sourceId(5L)
                .fileUrl("https://storage.example.com/biz.pdf")
                .build();
        given(ocrDocumentRepository.save(any(OcrDocument.class))).willReturn(savedDoc);
        given(ocrAdapter.extractText(anyString()))
                .willThrow(new RuntimeException("OCR 엔진 타임아웃"));

        OcrDocumentDto.Detail result = ocrService.extract(request);

        assertThat(result.status()).isEqualTo(OcrDocumentStatus.FAILED);
    }

    // ── search() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("OCR 키워드 검색 — 매칭 문서 반환")
    void search_returnsMatchingDocuments() {
        OcrDocument doc = OcrDocument.builder()
                .sourceType("CALLBACK_DOCUMENT")
                .sourceId(10L)
                .fileUrl("https://storage.example.com/doc.pdf")
                .build();
        doc.markExtracted("사업자등록번호: 123-45-67890 상호명: 주식회사 위즈캔", "/local/backup/ocr_10.txt");

        PageRequest pageable = PageRequest.of(0, 10);
        given(ocrDocumentRepository.searchByKeyword(eq("위즈캔"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(doc)));

        Page<OcrDocumentDto.SearchResult> result = ocrService.search("위즈캔", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).snippet()).contains("위즈캔");
        assertThat(result.getContent().get(0).sourceType()).isEqualTo("CALLBACK_DOCUMENT");
    }

    @Test
    @DisplayName("OCR 키워드 검색 — 결과 없음")
    void search_noResults_returnsEmptyPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        given(ocrDocumentRepository.searchByKeyword(eq("존재하지않는키워드"), eq(pageable)))
                .willReturn(new PageImpl<>(List.of()));

        Page<OcrDocumentDto.SearchResult> result = ocrService.search("존재하지않는키워드", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ── detail() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("단건 상세 조회 — 존재하는 OCR 문서 반환")
    void detail_returnsDocument() {
        OcrDocument doc = OcrDocument.builder()
                .sourceType("CALLBACK_DOCUMENT")
                .sourceId(10L)
                .fileUrl("https://storage.example.com/doc.pdf")
                .build();
        doc.markExtracted("추출된 텍스트", "/local/backup/ocr_1.txt");

        given(ocrDocumentRepository.findById(1L)).willReturn(Optional.of(doc));

        OcrDocumentDto.Detail result = ocrService.detail(1L);

        assertThat(result.sourceType()).isEqualTo("CALLBACK_DOCUMENT");
        assertThat(result.extractedText()).isEqualTo("추출된 텍스트");
        assertThat(result.localBackupPath()).isEqualTo("/local/backup/ocr_1.txt");
    }

    @Test
    @DisplayName("단건 상세 조회 — 존재하지 않으면 예외")
    void detail_notFound_throwsException() {
        given(ocrDocumentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ocrService.detail(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── OcrDocument 도메인 메서드 ────────────────────────────────────────

    @Test
    @DisplayName("OcrDocument.containsKeyword — 추출 텍스트 키워드 포함 여부")
    void ocrDocument_containsKeyword_caseInsensitive() {
        OcrDocument doc = OcrDocument.builder()
                .sourceType("CALLBACK_DOCUMENT")
                .sourceId(1L)
                .fileUrl("https://example.com/doc.pdf")
                .build();
        doc.markExtracted("사업자등록번호: 123-45-67890 대표자: 홍길동", "/backup/path.txt");

        assertThat(doc.containsKeyword("홍길동")).isTrue();
        assertThat(doc.containsKeyword("홍GIL동")).isFalse();
        assertThat(doc.containsKeyword("존재안함")).isFalse();
        assertThat(doc.containsKeyword(null)).isFalse();
    }
}
