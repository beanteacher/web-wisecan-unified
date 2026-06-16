package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.BalanceLedgerReason;
import com.wisecan.unified.domain.billing.ChargeBalanceLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChargeBalanceLedgerRepository extends JpaRepository<ChargeBalanceLedger, Long> {

    List<ChargeBalanceLedger> findByChargeBalanceIdOrderByRecordedAtAsc(Long chargeBalanceId);

    /**
     * 외부 발송 ID 멱등성 확인 — SEND 이유의 중복 차감 방지.
     */
    Optional<ChargeBalanceLedger> findByExternalSendIdAndReason(String externalSendId,
                                                                  BalanceLedgerReason reason);

    /**
     * 특정 ChargeBalance 의 순 변동 합산.
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM ChargeBalanceLedger l " +
           "WHERE l.chargeBalanceId = :chargeBalanceId")
    Long sumAmountByChargeBalanceId(@Param("chargeBalanceId") Long chargeBalanceId);
}
