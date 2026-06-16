package com.wisecan.unified.service.trial;

/**
 * 체험 세션 만료 예외 (W-406).
 *
 * <p>만료되었거나 종료된 세션으로 발송/결제 시도 시 발생한다.</p>
 */
public class TrialSessionExpiredException extends RuntimeException {

    public TrialSessionExpiredException(String message) {
        super(message);
    }
}
