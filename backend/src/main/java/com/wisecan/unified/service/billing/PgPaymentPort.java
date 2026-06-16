package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.PaymentMethodType;

/**
 * PG 결제 포트 (Adapter 패턴) — 외부 PG 연동 인터페이스.
 * MVP 구현체: StubPgPaymentAdapter (@ConditionalOnProperty pg.stub=true).
 * 실 PG 연동 시 이 인터페이스의 별도 구현체를 등록하면 되어 교체 부담 없음.
 */
public interface PgPaymentPort {

    /**
     * 단건 결제 요청.
     *
     * @param memberId       회원 ID
     * @param methodType     결제수단 유형
     * @param pgBillingKey   자동충전용 빌키 (수동 충전은 null)
     * @param amount         결제 금액 (원화 정수)
     * @param idempotencyKey 멱등성 키 — PG 측에도 같은 key로 요청, 중복 방지
     * @return PgPaymentResult 성공/실패 포함 결과
     */
    PgPaymentResult requestPayment(Long memberId, PaymentMethodType methodType,
                                   String pgBillingKey, long amount, String idempotencyKey);

    /**
     * 결제 취소 (환불).
     *
     * @param pgTxId 취소 대상 PG 거래번호
     * @param amount 취소 금액 (부분 취소 지원)
     * @return 취소 성공 여부
     */
    boolean cancelPayment(String pgTxId, long amount);

    /** PG 결제 결과 VO */
    record PgPaymentResult(boolean success, String pgTxId, String failReason) {

        public static PgPaymentResult success(String pgTxId) {
            return new PgPaymentResult(true, pgTxId, null);
        }

        public static PgPaymentResult failure(String reason) {
            return new PgPaymentResult(false, null, reason);
        }
    }
}
