package com.wisecan.unified.repository;

import com.wisecan.unified.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByBizNumber(String bizNumber);

    Optional<Company> findByBizNumber(String bizNumber);
}
