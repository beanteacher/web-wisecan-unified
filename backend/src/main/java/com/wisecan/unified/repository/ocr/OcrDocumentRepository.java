package com.wisecan.unified.repository.ocr;

import com.wisecan.unified.domain.ocr.OcrDocument;
import com.wisecan.unified.domain.ocr.OcrDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OcrDocumentRepository extends JpaRepository<OcrDocument, Long> {

    Optional<OcrDocument> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<OcrDocument> findByStatus(OcrDocumentStatus status);

    /**
     * 추출된 텍스트에서 키워드를 포함하는 문서를 검색한다 (§12.2 OCR 텍스트 키워드 검색).
     */
    @Query("SELECT d FROM OcrDocument d WHERE d.status = 'EXTRACTED' " +
           "AND LOWER(d.extractedText) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<OcrDocument> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
