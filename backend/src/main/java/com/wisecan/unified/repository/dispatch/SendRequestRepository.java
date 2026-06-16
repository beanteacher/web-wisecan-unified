package com.wisecan.unified.repository.dispatch;

import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 발송 요청 적재 레포지토리.
 *
 * <p>적재 P95 ≤ 1s 목표를 위해 인덱스 설계는 {@link SendRequest} 엔티티의
 * {@code @Table(indexes = ...)} 에 집중한다.
 * 조회는 send_id(ULID) 단일 식별자 경로를 기본으로 한다.</p>
 */
public interface SendRequestRepository extends JpaRepository<SendRequest, Long> {

    /**
     * ULID 기반 단일 조회.
     * API 응답·외부 시스템 연계에서 send_id만 노출하므로 가장 빈번한 조회 경로.
     */
    Optional<SendRequest> findBySendId(String sendId);

    /**
     * 회원별 발송 요청 목록 (최신순).
     * /histories 화면 조회 기반 — 외부 _tran/_log 조회로 대체될 예정이나
     * 내부 상태 추적용으로 유지한다.
     */
    Page<SendRequest> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * API Key별 발송 요청 목록 (최신순).
     * 일일 한도 초과 여부 확인 및 키별 사용 이력 조회에 사용.
     */
    Page<SendRequest> findByApiKeyIdOrderByCreatedAtDesc(Long apiKeyId, Pageable pageable);

    /**
     * 상태별 건수 조회 — 모니터링·재시도 배치용.
     */
    long countByStatus(SendRequestStatus status);

    /**
     * send_id 존재 여부 확인 — ULID 충돌 방어 (실제로는 발생 가능성 극히 낮음).
     */
    boolean existsBySendId(String sendId);

    /**
     * 회원의 예약 발송 목록 — PENDING 상태이고 requestedAt이 미래인 것만 (예약 발송 콘솔 화면).
     * W-206: 웹 콘솔 예약 발송 목록 조회에 사용.
     */
    Page<SendRequest> findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
            Long memberId, SendRequestStatus status, java.time.LocalDateTime after, Pageable pageable);
}
