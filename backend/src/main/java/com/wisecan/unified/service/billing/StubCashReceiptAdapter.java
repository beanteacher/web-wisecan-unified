package com.wisecan.unified.service.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 현금영수증 Stub 어댑터 — 개발/테스트 환경용.
 * cash-receipt.stub=true 일 때 활성화 (운영은 실 국세청 API 어댑터로 교체).
 */
@Component
@ConditionalOnProperty(name = "cash-receipt.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubCashReceiptAdapter implements CashReceiptPort {

    @Override
    public CashReceiptResult cancelCashReceipt(String originalIssueNo, long amount) {
        log.info("[STUB] 현금영수증 마이너스 정정 발행: originalIssueNo={}, amount={}", originalIssueNo, amount);
        return CashReceiptResult.success("STUB-CANCEL-" + originalIssueNo);
    }
}
