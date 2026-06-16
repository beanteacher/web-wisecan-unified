package com.wisecan.unified.service.trial;

/**
 * 체험 세션 어뷰징 차단 예외 (W-406).
 *
 * <p>동일 IP에서 단기간 내 임계치를 초과한 세션 발급 시도 시 발생한다.</p>
 */
public class TrialAbuseBlockedException extends RuntimeException {

    public TrialAbuseBlockedException(String message) {
        super(message);
    }
}
