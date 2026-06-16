package com.wisecan.unified.domain.dispatch;

/**
 * 발송 정합성 검증 실패 예외.
 * 9종 검증 게이트 중 하나라도 실패하면 이 예외를 던진다.
 * GlobalExceptionHandler 의 IllegalStateException 핸들러(409)가 포착하므로
 * 검증 실패는 HTTP 409 로 응답된다.
 */
public class SendValidationException extends IllegalStateException {

    private final SendErrorCode errorCode;

    public SendValidationException(SendErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public SendValidationException(SendErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public SendErrorCode getErrorCode() {
        return errorCode;
    }
}
