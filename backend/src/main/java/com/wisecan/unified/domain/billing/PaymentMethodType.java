package com.wisecan.unified.domain.billing;

/**
 * 결제수단 7종 — 02_FEATURE_SPEC §10.1.
 * 무통장입금(BANK_DEPOSIT)은 미지원.
 */
public enum PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    VIRTUAL_ACCOUNT,
    MOBILE,
    GIFT_CARD,
    POINT
}
