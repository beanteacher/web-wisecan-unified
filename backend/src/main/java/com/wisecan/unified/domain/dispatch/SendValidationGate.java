package com.wisecan.unified.domain.dispatch;

/**
 * 발송 전 정합성 검증 게이트 인터페이스.
 * 각 구현체는 단일 검증 책임만 진다.
 * 검증 실패 시 {@link SendValidationException}을 던진다.
 */
public interface SendValidationGate {

    /**
     * 검증을 수행한다.
     * @param ctx 발송 요청 컨텍스트
     * @throws SendValidationException 검증 실패 시
     */
    void validate(SendValidationContext ctx);

    /**
     * 검증 게이트 순서 (낮을수록 먼저 실행).
     * 기본값 0 — 순서가 중요한 게이트는 명시적으로 오버라이드한다.
     */
    default int order() {
        return 0;
    }
}
