package com.wisecan.unified.service.billing;

/**
 * 현금영수증 마이너스 정정 발행 Port — 02_FEATURE_SPEC §10.4·10.5.
 *
 * 환불 승인 시 현금영수증이 발급된 건에 대해 마이너스 정정을 자동 발행한다.
 * 실제 구현은 국세청 현금영수증 API 어댑터(StubCashReceiptAdapter)로 대체.
 */
public interface CashReceiptPort {

    /**
     * 마이너스 현금영수증 정정 발행.
     *
     * @param originalIssueNo 원본 현금영수증 국세청 발행 번호
     * @param amount          정정 금액 (원화 정수, 양수로 전달)
     * @return 발행 결과
     */
    CashReceiptResult cancelCashReceipt(String originalIssueNo, long amount);

    record CashReceiptResult(boolean success, String issueNo, String failReason) {

        public static CashReceiptResult success(String issueNo) {
            return new CashReceiptResult(true, issueNo, null);
        }

        public static CashReceiptResult failure(String reason) {
            return new CashReceiptResult(false, null, reason);
        }
    }
}
