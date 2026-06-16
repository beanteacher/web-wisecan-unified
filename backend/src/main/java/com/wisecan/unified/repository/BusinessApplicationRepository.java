package com.wisecan.unified.repository;

import com.wisecan.unified.domain.BusinessApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication, Long> {

    Optional<BusinessApplication> findByMemberIdAndStatus(Long memberId, String status);

    List<BusinessApplication> findByStatus(String status);

    /** 운영자 검토 큐: 복수 상태 조회 (§12.1) */
    List<BusinessApplication> findByStatusIn(List<String> statuses);

    boolean existsByMemberIdAndStatusIn(Long memberId, List<String> statuses);
}
