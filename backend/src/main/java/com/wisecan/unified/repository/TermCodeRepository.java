package com.wisecan.unified.repository;

import com.wisecan.unified.domain.TermCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermCodeRepository extends JpaRepository<TermCode, Long> {

    Optional<TermCode> findByCode(String code);

    List<TermCode> findByStatus(String status);
}
