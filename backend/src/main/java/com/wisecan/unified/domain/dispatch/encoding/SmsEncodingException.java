package com.wisecan.unified.domain.dispatch.encoding;

/**
 * SMS 인코딩 변환 실패 예외.
 *
 * <p>EUC-KR 변환 불가 문자 포함 또는 바이트 길이 초과 시 던진다.
 * GlobalExceptionHandler 에서 400 Bad Request 로 매핑한다.</p>
 */
public class SmsEncodingException extends IllegalArgumentException {

    public SmsEncodingException(String message) {
        super(message);
    }
}
