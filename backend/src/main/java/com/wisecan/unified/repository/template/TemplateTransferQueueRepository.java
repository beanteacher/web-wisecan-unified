package com.wisecan.unified.repository.template;

import com.wisecan.unified.domain.template.TemplateTransferQueue;
import com.wisecan.unified.domain.template.TemplateTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * SMS17 이관 처리 큐 Repository.
 */
public interface TemplateTransferQueueRepository extends JpaRepository<TemplateTransferQueue, Long> {

    /** 회원의 이관 신청 목록 조회 (최신순) */
    List<TemplateTransferQueue> findByMemberIdOrderByRequestedAtDesc(Long memberId);

    /** 특정 상태의 이관 신청 목록 (운영자 큐) */
    List<TemplateTransferQueue> findByStatusOrderByRequestedAtAsc(TemplateTransferStatus status);

    /** 회원의 특정 소스 템플릿 코드 이관 신청 중복 확인 */
    boolean existsByMemberIdAndSourceTemplateCodeAndStatusIn(
            Long memberId, String sourceTemplateCode, List<TemplateTransferStatus> statuses);
}
