package com.wisecan.unified.service.billing;

/**
 * Redis 잔액 캐시 무효화 이벤트 — W-401.
 * AFTER_COMMIT 리스너가 수신하여 balance:{memberId} 키를 삭제한다.
 * write-around 전략: DB 기준이 진실 원천, 캐시는 만료 후 재조회.
 */
public record BalanceCacheEvictEvent(Long memberId) {}
