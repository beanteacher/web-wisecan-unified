package com.wisecan.unified.repository.dispatch;

import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자 대시보드·통계 집계 전용 리포지토리 — W-503
 *
 * <p>SendRequestRepository 와 역할 분리:
 * SendRequestRepository — 발송 요청 CRUD + 단건 조회
 * AdminSendStatsRepository — 운영자 통계 집계 전용 (읽기 전용 집계 쿼리)</p>
 */
public interface AdminSendStatsRepository extends JpaRepository<SendRequest, Long> {

    // ── 일별 집계 ─────────────────────────────────────────────────────

    /**
     * 기간 내 총 발송 건수.
     */
    @Query("SELECT COUNT(s) FROM SendRequest s WHERE s.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 기간 내 총 수신자 수.
     */
    @Query("SELECT COALESCE(SUM(s.recipientCount), 0) FROM SendRequest s WHERE s.createdAt BETWEEN :from AND :to")
    long sumRecipientCountByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 기간 내 총 매출 (차감 금액 합계).
     */
    @Query("SELECT COALESCE(SUM(s.totalCost), 0) FROM SendRequest s WHERE s.createdAt BETWEEN :from AND :to")
    long sumTotalCostByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 상태별 건수.
     */
    long countByStatus(SendRequestStatus status);

    /**
     * 일별 발송 집계 — (날짜, 건수, 수신자수, 총비용).
     */
    @Query("""
        SELECT CAST(s.createdAt AS LocalDate),
               COUNT(s),
               COALESCE(SUM(s.recipientCount), 0),
               COALESCE(SUM(s.totalCost), 0)
        FROM SendRequest s
        WHERE s.createdAt BETWEEN :from AND :to
        GROUP BY CAST(s.createdAt AS LocalDate)
        ORDER BY CAST(s.createdAt AS LocalDate) ASC
        """)
    List<Object[]> aggregateDailyByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 채널별 발송 집계.
     */
    @Query("""
        SELECT s.channel,
               COUNT(s),
               COALESCE(SUM(s.recipientCount), 0),
               COALESCE(SUM(s.totalCost), 0)
        FROM SendRequest s
        WHERE s.createdAt BETWEEN :from AND :to
        GROUP BY s.channel
        ORDER BY COUNT(s) DESC
        """)
    List<Object[]> aggregateByChannelBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
