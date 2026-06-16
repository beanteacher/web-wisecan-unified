package com.wisecan.unified.repository;

import com.wisecan.unified.domain.TermAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermAgreementRepository extends JpaRepository<TermAgreement, Long> {

    List<TermAgreement> findByMemberId(Long memberId);

    boolean existsByMemberIdAndTermCodeId(Long memberId, Long termCodeId);
}
