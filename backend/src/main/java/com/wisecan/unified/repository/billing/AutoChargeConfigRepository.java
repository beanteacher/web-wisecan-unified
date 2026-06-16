package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.AutoChargeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AutoChargeConfigRepository extends JpaRepository<AutoChargeConfig, Long> {

    Optional<AutoChargeConfig> findByMemberId(Long memberId);

    /**
     * 트리거 대상 자동충전 설정 조회 — 활성 상태이고 만료되지 않은 것만.
     * (만료일 NULL 또는 오늘 이후 포함)
     */
    @Query("SELECT a FROM AutoChargeConfig a " +
           "WHERE a.enabledYn = 'Y' " +
           "AND (a.expiresAt IS NULL OR a.expiresAt >= :today)")
    List<AutoChargeConfig> findAllActive(LocalDate today);
}
