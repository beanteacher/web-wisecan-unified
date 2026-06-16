package com.wisecan.unified.service.ocr;

import com.wisecan.unified.domain.ocr.OcrAdapter;
import com.wisecan.unified.domain.ocr.OcrDocument;
import com.wisecan.unified.dto.ocr.OcrDocumentDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ocr.OcrDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * OCR 서비스 (§12.2).
 * - 문서 텍스트 추출: OcrAdapter를 통해 실 OCR 또는 Stub을 사용한다.
 * - 키워드 검색: 추출된 텍스트에서 운영자가 입력한 키워드로 검색.
 * - 이중 보관: 추출 결과를 DB(클라우드) + 로컬 파일에 저장.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private static final String LOCAL_OCR_DIR = "ocr-exports";

    private final OcrDocumentRepository ocrDocumentRepository;
    private final OcrAdapter ocrAdapter;

    /**
     * 문서 텍스트 추출 요청 — OCR 어댑터 호출 후 결과 저장 (이중 보관).
     */
    @Transactional
    public OcrDocumentDto.Detail extract(OcrDocumentDto.ExtractRequest request) {
        // 이미 추출된 경우 기존 결과 반환
        return ocrDocumentRepository
                .findBySourceTypeAndSourceId(request.sourceType(), request.sourceId())
                .map(OcrDocumentDto.Detail::from)
                .orElseGet(() -> doExtract(request));
    }

    private OcrDocumentDto.Detail doExtract(OcrDocumentDto.ExtractRequest request) {
        OcrDocument doc = OcrDocument.builder()
                .sourceType(request.sourceType())
                .sourceId(request.sourceId())
                .fileUrl(request.fileUrl())
                .build();
        ocrDocumentRepository.save(doc);

        try {
            String text = ocrAdapter.extractText(request.fileUrl());
            String localPath = saveLocalBackup(doc.getId(), text);
            doc.markExtracted(text, localPath);
            log.info("[OCR] 추출 완료 — docId={}, sourceType={}, sourceId={}",
                    doc.getId(), request.sourceType(), request.sourceId());
        } catch (Exception e) {
            doc.markFailed(e.getMessage());
            log.error("[OCR] 추출 실패 — docId={}, error={}", doc.getId(), e.getMessage());
        }

        ocrDocumentRepository.save(doc);
        return OcrDocumentDto.Detail.from(doc);
    }

    /** OCR 텍스트 키워드 검색 (§12.2 발신번호 통합관리 화면) */
    @Transactional(readOnly = true)
    public Page<OcrDocumentDto.SearchResult> search(String keyword, Pageable pageable) {
        return ocrDocumentRepository.searchByKeyword(keyword, pageable)
                .map(doc -> OcrDocumentDto.SearchResult.from(doc, keyword));
    }

    /** 단건 상세 조회 */
    @Transactional(readOnly = true)
    public OcrDocumentDto.Detail detail(Long id) {
        OcrDocument doc = ocrDocumentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("OcrDocument", id));
        return OcrDocumentDto.Detail.from(doc);
    }

    /** 로컬 파일 백업 저장 — 이중 보관 */
    private String saveLocalBackup(Long docId, String text) throws IOException {
        String fileName = "ocr_" + docId + "_"
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".txt";
        Path path = Paths.get(LOCAL_OCR_DIR, fileName);
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
        return path.toString();
    }
}
