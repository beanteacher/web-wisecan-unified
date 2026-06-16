package com.wisecan.unified.exception;

/**
 * 잔액 부족 — 분기 3(부분 발송·전체 취소) 선택지 제시 후 취소를 선택한 경우 HTTP 402.
 *
 * W-405: 자동결제·후불 모두 비활성이고 회원이 전체 취소를 선택할 때 사용.
 */
public class InsufficientFundsException extends RuntimeException {

    private final long shortfall;

    public InsufficientFundsException(long shortfall) {
        super("잔액이 부족합니다. 부족액: " + shortfall + "원");
        this.shortfall = shortfall;
    }

    public long getShortfall() {
        return shortfall;
    }
}
