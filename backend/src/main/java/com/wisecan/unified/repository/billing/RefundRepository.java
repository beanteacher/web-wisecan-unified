package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.Refund;
import com.wisecan.unified.domain.billing.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    /** 회원의 환불 목록 (최신순) */
    List<Refund> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    /** 운영자 대기 목록 — PENDING 상태 전체 (페이지네이션) */
    Page<Refund> findByStatusOrderByRequestedAtAsc(RefundStatus status, Pageable pageable);

    /** 특정 ChargeBalance 에 대한 처리 중 환불 존재 여부 (중복 신청 방지) */
    boolean existsByChargeBalanceIdAndStatusIn(Long chargeBalanceId, List<RefundStatus> statuses);
}
