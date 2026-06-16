package com.wisecan.unified.repository;

import com.wisecan.unified.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자 회원 통계 집계 전용 리포지토리 — W-503
 */
public interface AdminMemberStatsRepository extends JpaRepository<Member, Long> {

    /**
     * 전체 회원 수.
     */
    long count();

    /**
     * 기간 내 신규 가입 회원 수.
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 일별 신규 회원 집계 — (날짜, 신규 수).
     */
    @Query("""
        SELECT CAST(m.createdAt AS LocalDate),
               COUNT(m)
        FROM Member m
        WHERE m.createdAt BETWEEN :from AND :to
        GROUP BY CAST(m.createdAt AS LocalDate)
        ORDER BY CAST(m.createdAt AS LocalDate) ASC
        """)
    List<Object[]> aggregateDailyNewMembersBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * 특정 일시 이전 회원 총수 (누적 계산용).
     */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.createdAt <= :before")
    long countByCreatedAtBefore(@Param("before") LocalDateTime before);
}
