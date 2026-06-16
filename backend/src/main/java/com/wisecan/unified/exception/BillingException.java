package com.wisecan.unified.exception;

/**
 * 과금/충전 도메인 비즈니스 예외.
 * GlobalExceptionHandler 에서 400 Bad Request 로 매핑.
 */
public class BillingException extends RuntimeException {

    public BillingException(String message) {
        super(message);
    }

    public BillingException(String message, Throwable cause) {
        super(message, cause);
    }
}
