package com.wisecan.unified.exception;

/**
 * 2차 인증이 필요한 상태 — 1차 자격 증명은 통과했으나
 * 클라이언트가 2차 인증 코드를 제출해야 최종 토큰을 발급받을 수 있다.
 *
 * HTTP 202 Accepted 로 응답하고, body에 mfaToken(임시 토큰)을 포함한다.
 */
public class TwoFactorRequiredException extends RuntimeException {

    private final String mfaToken;

    public TwoFactorRequiredException(String mfaToken) {
        super("2차 인증이 필요합니다.");
        this.mfaToken = mfaToken;
    }

    public String getMfaToken() {
        return mfaToken;
    }
}
