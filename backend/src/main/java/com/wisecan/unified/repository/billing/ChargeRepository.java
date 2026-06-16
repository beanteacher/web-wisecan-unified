package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.Charge;
import com.wisecan.unified.domain.billing.ChargeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChargeRepository extends JpaRepository<Charge, Long> {

    /** PG 거래번호 멱등성 조회 */
    Optional<Charge> findByPgTxId(String pgTxId);

    Page<Charge> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<Charge> findByMemberIdAndStatusOrderByCreatedAtDesc(Long memberId, ChargeStatus status, Pageable pageable);
}
