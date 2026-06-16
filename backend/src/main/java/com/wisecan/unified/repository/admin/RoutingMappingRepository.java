package com.wisecan.unified.repository.admin;

import com.wisecan.unified.domain.admin.RoutingChannel;
import com.wisecan.unified.domain.admin.RoutingMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutingMappingRepository extends JpaRepository<RoutingMapping, Long> {

    /** 회원의 특정 채널 매핑 조회 */
    Optional<RoutingMapping> findByMemberIdAndChannel(Long memberId, RoutingChannel channel);

    /** 회원의 전체 채널 매핑 목록 */
    List<RoutingMapping> findByMemberId(Long memberId);

    /** 특정 채널의 전체 매핑 목록 (운영자 일괄 조회) */
    List<RoutingMapping> findByChannel(RoutingChannel channel);
}
