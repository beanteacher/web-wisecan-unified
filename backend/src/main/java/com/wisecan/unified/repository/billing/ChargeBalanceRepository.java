package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.ChargeBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChargeBalanceRepository extends JpaRepository<ChargeBalance, Long> {

    /**
     * FIFO + 만료 임박 우선 잔액 조회 (차감 대상).
     * amountRemaining > 0 && not expired 조건.
     */
    @Query("SELECT cb FROM ChargeBalance cb " +
           "WHERE cb.memberId = :memberId " +
           "AND cb.amountRemaining > 0 " +
           "AND cb.expiresAt > :now " +
           "ORDER BY cb.expiresAt ASC, cb.chargedAt ASC")
    List<ChargeBalance> findDeductibleByMemberId(@Param("memberId") Long memberId,
                                                  @Param("now") LocalDateTime now);

    /**
     * 회원 전체 잔액 합산 — Redis 캐시 MISS 시 DB fallback.
     */
    @Query("SELECT COALESCE(SUM(cb.amountRemaining), 0) FROM ChargeBalance cb " +
           "WHERE cb.memberId = :memberId " +
           "AND cb.amountRemaining > 0 " +
           "AND cb.expiresAt > :now")
    Long sumRemainingByMemberId(@Param("memberId") Long memberId,
                                 @Param("now") LocalDateTime now);

    /**
     * 만료 처리 대상 조회 (배치/스케줄러용).
     */
    @Query("SELECT cb FROM ChargeBalance cb " +
           "WHERE cb.amountRemaining > 0 " +
           "AND cb.expiresAt <= :now")
    List<ChargeBalance> findExpired(@Param("now") LocalDateTime now);

    /**
     * 회원의 전체 잔액 행 — 만료 임박 우선 정렬 (운영자 강제 차감용, W-501 §12.5).
     */
    @Query("SELECT cb FROM ChargeBalance cb " +
           "WHERE cb.memberId = :memberId " +
           "AND cb.amountRemaining > 0 " +
           "ORDER BY cb.expiresAt ASC, cb.chargedAt ASC")
    List<ChargeBalance> findByMemberIdOrderByExpiresAtAscChargedAtAsc(@Param("memberId") Long memberId);

    /**
     * 기간별 충전 잔액 행 조회 (정산 보고서용, W-501 §12.5).
     */
    @Query("SELECT cb FROM ChargeBalance cb " +
           "WHERE cb.chargedAt >= :periodStart AND cb.chargedAt <= :periodEnd")
    List<ChargeBalance> findByChargedAtBetween(@Param("periodStart") LocalDateTime periodStart,
                                               @Param("periodEnd") LocalDateTime periodEnd);
}
