package com.wisecan.unified.domain.dispatch;

/**
 * 발송 요청 적재 상태.
 *
 * <p>외부 발송 시스템의 {@code message_state}와 별개로,
 * 본 서비스 내부 적재 라이프사이클을 표현한다.</p>
 *
 * <ul>
 *   <li>PENDING   — 적재 전 (검증·인코딩 완료, 외부 INSERT 대기)</li>
 *   <li>QUEUED    — 외부 발송 시스템에 INSERT 완료 ({@code message_state=0})</li>
 *   <li>FAILED    — 외부 INSERT 실패 (보상 트랜잭션 대상)</li>
 *   <li>CANCELLED — 잔액 부족 등으로 최종 거부</li>
 * </ul>
 */
public enum SendRequestStatus {

    /** 외부 적재 대기 — 검증·인코딩 완료 후 INSERT 직전 */
    PENDING,

    /** 외부 발송 시스템 INSERT 완료 */
    QUEUED,

    /** 외부 INSERT 실패 — 보상 트랜잭션(REVERT) 처리 대상 */
    FAILED,

    /** 잔액 부족·정책 위반 등으로 최종 거부 */
    CANCELLED
}
