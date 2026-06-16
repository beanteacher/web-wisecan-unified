package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.Refund;
import com.wisecan.unified.domain.billing.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    /** 회원의 환불 목록 (최신순) */
    List<Refund> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    /** 운영자 대기 목록 — PENDING 상태 전체 (페이지네이션) */
    Page<Refund> findByStatusOrderByRequestedAtAsc(RefundStatus status, Pageable pageable);

    /** 특정 ChargeBalance 에 대한 처리 중 환불 존재 여부 (중복 신청 방지) */
    boolean existsByChargeBalanceIdAndStatusIn(Long chargeBalanceId, List<RefundStatus> statuses);

    /** 운영자 전체 환불 목록 — 최신순 (W-501 §12.5) */
    List<Refund> findAllByOrderByRequestedAtDesc();

    /**
     * 기간 내 특정 회원 집합의 환불 목록 — 정산 보고서용 일괄 조회 (H-2).
     * N+1 방지: 루프 내 개별 조회 대신 단일 IN 쿼리로 대체.
     */
    List<Refund> findByMemberIdInAndRequestedAtBetween(Collection<Long> memberIds,
                                                        LocalDateTime from,
                                                        LocalDateTime to);
}
