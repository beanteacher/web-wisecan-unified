package com.wisecan.unified.repository;

import com.wisecan.unified.domain.CompanySubAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanySubAccountRepository extends JpaRepository<CompanySubAccount, Long> {

    List<CompanySubAccount> findByCompanyIdAndStatusNot(Long companyId, String status);

    Optional<CompanySubAccount> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    List<CompanySubAccount> findByCompanyIdAndStatus(Long companyId, String status);
}
