package com.wisecan.unified.repository.billing;

import com.wisecan.unified.domain.billing.PostpaidConfig;
import com.wisecan.unified.domain.billing.PostpaidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 후불 설정 Repository — 05_DATA_MODEL §7.4.
 */
public interface PostpaidConfigRepository extends JpaRepository<PostpaidConfig, Long> {

    Optional<PostpaidConfig> findByCompanyId(Long companyId);

    boolean existsByCompanyIdAndStatus(Long companyId, PostpaidStatus status);
}
