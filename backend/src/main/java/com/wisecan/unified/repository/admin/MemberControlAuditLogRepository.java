package com.wisecan.unified.repository.admin;

import com.wisecan.unified.domain.admin.MemberControlAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberControlAuditLogRepository extends JpaRepository<MemberControlAuditLog, Long> {

    /** 특정 회원의 통제 감사 이력 (최신순) */
    List<MemberControlAuditLog> findByMemberIdOrderByOccurredAtDesc(Long memberId);

    /** 특정 운영자의 처리 이력 (최신순) */
    List<MemberControlAuditLog> findByOperatorIdOrderByOccurredAtDesc(Long operatorId);
}
