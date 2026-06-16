package com.wisecan.unified.repository;

import com.wisecan.unified.domain.CompanyRoleLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRoleLogRepository extends JpaRepository<CompanyRoleLog, Long> {

    List<CompanyRoleLog> findByCompanyIdOrderByActedAtDesc(Long companyId);
}
