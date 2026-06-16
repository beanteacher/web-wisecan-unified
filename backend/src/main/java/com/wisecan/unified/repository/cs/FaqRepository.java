package com.wisecan.unified.repository.cs;

import com.wisecan.unified.domain.cs.Faq;
import com.wisecan.unified.domain.cs.InquiryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    /** 노출 FAQ 전체 — 정렬 순서 오름차순 */
    List<Faq> findByVisibleTrueOrderBySortOrderAsc();

    /** 카테고리별 노출 FAQ */
    List<Faq> findByCategoryAndVisibleTrueOrderBySortOrderAsc(InquiryCategory category);

    /** 관리자용 전체 FAQ (노출 여부 무관) */
    List<Faq> findAllByOrderBySortOrderAsc();

    /** 챗봇 매칭용: 질문에 키워드 포함 + 노출 중인 FAQ */
    List<Faq> findByQuestionContainingIgnoreCaseAndVisibleTrue(String keyword);
}
