package com.wisecan.unified.repository.cs;

import com.wisecan.unified.domain.cs.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 회원 노출용: visible=true, pinned 우선 정렬 */
    List<Notice> findByVisibleTrueOrderByPinnedDescCreatedAtDesc();

    /** 관리자용 전체 목록 */
    Page<Notice> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);
}
