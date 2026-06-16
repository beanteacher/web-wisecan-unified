package com.wisecan.unified.domain.dispatch;

/**
 * 발송 망 유형 — 테스트망 / 상용망.
 *
 * <p>API Key의 {@link com.wisecan.unified.domain.ApiKeyType}과 1:1 대응한다.</p>
 * <ul>
 *   <li>TEST       — 테스트망. 실제 캐시 미차감, 채널별 쿼터 적용.</li>
 *   <li>PRODUCTION — 상용망. 실사용 캐시 차감, 외부 발송 시스템 실 경로 사용.</li>
 * </ul>
 *
 * W-205: 테스트 키로 상용 발송 시도 시 {@link NetworkRoutingGate}가 거부한다.
 */
public enum NetworkType {
    TEST,
    PRODUCTION
}
