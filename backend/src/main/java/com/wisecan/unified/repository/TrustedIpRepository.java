package com.wisecan.unified.repository;

import com.wisecan.unified.domain.TrustedIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrustedIpRepository extends JpaRepository<TrustedIp, Long> {

    List<TrustedIp> findByMemberId(Long memberId);

    boolean existsByMemberIdAndIpAddress(Long memberId, String ipAddress);

    int countByMemberId(Long memberId);

    void deleteByMemberIdAndId(Long memberId, Long id);
}
