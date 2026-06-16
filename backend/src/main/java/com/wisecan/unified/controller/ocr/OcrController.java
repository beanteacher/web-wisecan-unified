package com.wisecan.unified.controller.ocr;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.ocr.OcrDocumentDto;
import com.wisecan.unified.service.ocr.OcrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OCR 문서 텍스트 추출·검색 API (§12.2) — ADMIN 전용.
 *
 * POST /admin/ocr/extract           문서 텍스트 추출 요청
 * GET  /admin/ocr/search?keyword=.. 추출된 텍스트 키워드 검색
 * GET  /admin/ocr/{id}              OCR 결과 단건 상세
 */
@RestController
@RequestMapping("/admin/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<OcrDocumentDto.Detail>> extract(
            @RequestBody @Valid OcrDocumentDto.ExtractRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ocrService.extract(request)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<OcrDocumentDto.SearchResult>>> search(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ocrService.search(keyword, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OcrDocumentDto.Detail>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ocrService.detail(id)));
    }
}
