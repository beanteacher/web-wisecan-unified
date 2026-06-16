package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.PostpaidInvoice;
import com.wisecan.unified.domain.billing.PostpaidInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 후불 청구서 Repository — 05_DATA_MODEL §7.4.
 */
public interface PostpaidInvoiceRepository extends JpaRepository<PostpaidInvoice, Long> {

    Optional<PostpaidInvoice> findByPostpaidConfigIdAndPeriodLabel(Long postpaidConfigId, String periodLabel);

    List<PostpaidInvoice> findByPostpaidConfigIdOrderByIssuedAtDesc(Long postpaidConfigId);

    /** 연체 처리 대상 — ISSUED 상태이고 dueAt 이 기준 시각 이전인 건 */
    @Query("SELECT i FROM PostpaidInvoice i " +
           "WHERE i.status = 'ISSUED' AND i.dueAt < :now")
    List<PostpaidInvoice> findOverdueTargets(@Param("now") LocalDateTime now);

    /** 특정 config 의 연체 청구서 존재 여부 (PostpaidBlockGate 연계용) */
    boolean existsByPostpaidConfigIdAndStatus(Long postpaidConfigId, PostpaidInvoiceStatus status);

    /** companyId 기준 연체 청구서 존재 여부 (PostpaidBlockGate 에서 companyId 로 조회 시 활용) */
    @Query("SELECT COUNT(i) > 0 FROM PostpaidInvoice i " +
           "JOIN PostpaidConfig c ON i.postpaidConfigId = c.id " +
           "WHERE c.companyId = :companyId AND i.status = com.wisecan.unified.domain.billing.PostpaidInvoiceStatus.OVERDUE")
    boolean existsOverdueByCompanyId(@Param("companyId") Long companyId);
}
