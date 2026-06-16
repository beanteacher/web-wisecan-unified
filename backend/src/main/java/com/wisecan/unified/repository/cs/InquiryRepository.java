package com.wisecan.unified.repository.cs;

import com.wisecan.unified.domain.cs.Inquiry;
import com.wisecan.unified.domain.cs.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Inquiry> findByStatusOrderByCreatedAtDesc(InquiryStatus status, Pageable pageable);

    /** 24h SLA 측정: 답변 완료된 문의 중 지정 기간 내 건수 */
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.answeredAt IS NOT NULL " +
           "AND i.answeredAt >= :from AND i.answeredAt <= :to")
    long countAnsweredBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** SLA 초과 건수: answeredAt - createdAt > 24h */
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.answeredAt IS NOT NULL " +
           "AND i.answeredAt >= :from AND i.answeredAt <= :to " +
           "AND (TIMESTAMPDIFF(HOUR, i.createdAt, i.answeredAt)) > 24")
    long countSlaBreachedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 미답변 오픈 문의 목록 (OPEN 또는 IN_PROGRESS) */
    List<Inquiry> findByStatusInOrderByCreatedAtAsc(List<InquiryStatus> statuses);
}
