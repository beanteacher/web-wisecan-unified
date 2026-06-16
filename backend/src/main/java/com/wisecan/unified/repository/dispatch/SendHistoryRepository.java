package com.wisecan.unified.repository.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 발송 이력 조회 전용 Repository (W-304).
 *
 * <p>기존 {@link SendRequestRepository}는 발송 적재(W-203~W-206) 워커가 사용한다.
 * W-304는 별도 인터페이스로 격리하여 다른 워커와의 충돌을 최소화한다.</p>
 *
 * <p>키별 조회 범위 정책 (02_FEATURE_SPEC.md §5.3, §8.1):</p>
 * <ul>
 *   <li>scope:key — apiKeyId 기준 필터 (기본)</li>
 *   <li>scope:member — memberId 기준 필터 (회원 전체 키)</li>
 * </ul>
 */
public interface SendHistoryRepository extends JpaRepository<SendRequest, Long> {

    // ── send_id 단건 조회 ──────────────────────────────────────────────

    /**
     * ULID(send_id)로 단건 조회 — 조회 범위 정책은 서비스 레이어에서 적용.
     */
    Optional<SendRequest> findBySendId(String sendId);

    // ── scope:key — API Key 기준 ───────────────────────────────────────

    /**
     * API Key 기준 이력 목록 (필터 없음).
     */
    Page<SendRequest> findByApiKeyIdOrderByCreatedAtDesc(Long apiKeyId, Pageable pageable);

    /**
     * API Key 기준 이력 목록 + 채널 필터.
     */
    Page<SendRequest> findByApiKeyIdAndChannelOrderByCreatedAtDesc(
            Long apiKeyId, SendChannel channel, Pageable pageable);

    /**
     * API Key 기준 이력 목록 + 상태 필터.
     */
    Page<SendRequest> findByApiKeyIdAndStatusOrderByCreatedAtDesc(
            Long apiKeyId, SendRequestStatus status, Pageable pageable);

    /**
     * API Key 기준 이력 목록 + 채널 + 상태 필터.
     */
    Page<SendRequest> findByApiKeyIdAndChannelAndStatusOrderByCreatedAtDesc(
            Long apiKeyId, SendChannel channel, SendRequestStatus status, Pageable pageable);

    /**
     * API Key 기준 이력 목록 + 기간 + 채널 + 상태 + 발신번호 + 수신번호 포함 검색 (복합 필터).
     *
     * <p>발신번호·수신번호는 null이면 조건에서 제외한다 (JPQL coalesce 활용).</p>
     */
    @Query("""
            SELECT sr FROM SendRequest sr
            WHERE sr.apiKeyId = :apiKeyId
              AND (:fromDate IS NULL OR sr.createdAt >= :fromDate)
              AND (:toDate IS NULL OR sr.createdAt <= :toDate)
              AND (:channel IS NULL OR sr.channel = :channel)
              AND (:status IS NULL OR sr.status = :status)
              AND (:callbackNumber IS NULL OR sr.callbackNumber = :callbackNumber)
              AND (:recipientNumber IS NULL OR sr.recipientNumbers LIKE %:recipientNumber%)
            ORDER BY sr.createdAt DESC
            """)
    Page<SendRequest> findByApiKeyWithFilters(
            @Param("apiKeyId") Long apiKeyId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("channel") SendChannel channel,
            @Param("status") SendRequestStatus status,
            @Param("callbackNumber") String callbackNumber,
            @Param("recipientNumber") String recipientNumber,
            Pageable pageable);

    // ── scope:member — 회원 전체 키 기준 ──────────────────────────────

    /**
     * 회원 전체 키 기준 이력 목록 + 복합 필터.
     *
     * <p>scope:member 토글을 보유한 키에서만 호출 가능 (서비스 레이어에서 검증).</p>
     */
    @Query("""
            SELECT sr FROM SendRequest sr
            WHERE sr.memberId = :memberId
              AND (:fromDate IS NULL OR sr.createdAt >= :fromDate)
              AND (:toDate IS NULL OR sr.createdAt <= :toDate)
              AND (:channel IS NULL OR sr.channel = :channel)
              AND (:status IS NULL OR sr.status = :status)
              AND (:callbackNumber IS NULL OR sr.callbackNumber = :callbackNumber)
              AND (:recipientNumber IS NULL OR sr.recipientNumbers LIKE %:recipientNumber%)
            ORDER BY sr.createdAt DESC
            """)
    Page<SendRequest> findByMemberWithFilters(
            @Param("memberId") Long memberId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("channel") SendChannel channel,
            @Param("status") SendRequestStatus status,
            @Param("callbackNumber") String callbackNumber,
            @Param("recipientNumber") String recipientNumber,
            Pageable pageable);
}
