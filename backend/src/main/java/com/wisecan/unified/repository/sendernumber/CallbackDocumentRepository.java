package com.wisecan.unified.repository.sendernumber;

import com.wisecan.unified.domain.sendernumber.CallbackDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallbackDocumentRepository extends JpaRepository<CallbackDocument, Long> {

    List<CallbackDocument> findByCallbackId(Long callbackId);
}
