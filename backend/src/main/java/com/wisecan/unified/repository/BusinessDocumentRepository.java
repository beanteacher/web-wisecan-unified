package com.wisecan.unified.repository;

import com.wisecan.unified.domain.BusinessDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long> {

    List<BusinessDocument> findByApplicationId(Long applicationId);
}
