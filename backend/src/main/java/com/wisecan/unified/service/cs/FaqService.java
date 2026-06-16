package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.Faq;
import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.dto.cs.FaqDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.cs.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    /** 회원용: 노출 FAQ 전체 목록 */
    @Transactional(readOnly = true)
    public List<FaqDto.Response> listVisible() {
        return faqRepository.findByVisibleTrueOrderBySortOrderAsc()
                .stream().map(FaqDto.Response::from).toList();
    }

    /** 회원용: 카테고리별 노출 FAQ */
    @Transactional(readOnly = true)
    public List<FaqDto.Response> listByCategory(InquiryCategory category) {
        return faqRepository.findByCategoryAndVisibleTrueOrderBySortOrderAsc(category)
                .stream().map(FaqDto.Response::from).toList();
    }

    /** 관리자용: 전체 FAQ (노출 여부 무관) */
    @Transactional(readOnly = true)
    public List<FaqDto.Response> listAll() {
        return faqRepository.findAllByOrderBySortOrderAsc()
                .stream().map(FaqDto.Response::from).toList();
    }

    /** 관리자: FAQ 등록 */
    @Transactional
    public FaqDto.Response create(FaqDto.CreateRequest request) {
        Faq faq = Faq.builder()
                .category(request.category())
                .question(request.question())
                .answer(request.answer())
                .sortOrder(request.sortOrder())
                .visible(request.visible())
                .build();
        return FaqDto.Response.from(faqRepository.save(faq));
    }

    /** 관리자: FAQ 수정 */
    @Transactional
    public FaqDto.Response update(Long id, FaqDto.UpdateRequest request) {
        Faq faq = findById(id);
        faq.update(request.question(), request.answer(),
                request.category(), request.sortOrder(), request.visible());
        return FaqDto.Response.from(faq);
    }

    /** 관리자: FAQ 삭제 */
    @Transactional
    public void delete(Long id) {
        Faq faq = findById(id);
        faqRepository.delete(faq);
    }

    /** 챗봇 키워드 매칭용 */
    @Transactional(readOnly = true)
    public List<FaqDto.Response> searchByKeyword(String keyword) {
        return faqRepository.findByQuestionContainingIgnoreCaseAndVisibleTrue(keyword)
                .stream().map(FaqDto.Response::from).toList();
    }

    private Faq findById(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FAQ를 찾을 수 없습니다. id=" + id));
    }
}
