package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.Faq;
import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.dto.cs.FaqDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.cs.FaqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @Mock
    private FaqRepository faqRepository;

    @InjectMocks
    private FaqService faqService;

    @Test
    @DisplayName("FAQ 등록 — 저장 후 Response 반환")
    void create_success() {
        FaqDto.CreateRequest req = new FaqDto.CreateRequest(
                InquiryCategory.BILLING, "충전은 어떻게 하나요?", "대시보드에서 충전 가능합니다.", 1, true);
        Faq faq = Faq.builder()
                .category(InquiryCategory.BILLING)
                .question("충전은 어떻게 하나요?")
                .answer("대시보드에서 충전 가능합니다.")
                .sortOrder(1)
                .visible(true)
                .build();
        given(faqRepository.save(any(Faq.class))).willReturn(faq);

        FaqDto.Response result = faqService.create(req);

        assertThat(result.question()).isEqualTo("충전은 어떻게 하나요?");
        assertThat(result.visible()).isTrue();
    }

    @Test
    @DisplayName("노출 FAQ 목록 — visible=true 항목만 반환")
    void listVisible_returnsOnlyVisible() {
        Faq faq = Faq.builder()
                .category(InquiryCategory.ACCOUNT)
                .question("비밀번호 변경")
                .answer("설정 메뉴에서 변경 가능합니다.")
                .sortOrder(1)
                .visible(true)
                .build();
        given(faqRepository.findByVisibleTrueOrderBySortOrderAsc()).willReturn(List.of(faq));

        List<FaqDto.Response> result = faqService.listVisible();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(InquiryCategory.ACCOUNT);
    }

    @Test
    @DisplayName("FAQ 삭제 — 존재하지 않는 ID 시 예외")
    void delete_notFound_throws() {
        given(faqRepository.findById(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> faqService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("FAQ 수정 — 질문·답변·카테고리 업데이트")
    void update_success() {
        Faq faq = Faq.builder()
                .category(InquiryCategory.ETC)
                .question("기존 질문")
                .answer("기존 답변")
                .sortOrder(5)
                .visible(false)
                .build();
        given(faqRepository.findById(1L)).willReturn(Optional.of(faq));

        FaqDto.UpdateRequest req = new FaqDto.UpdateRequest(
                InquiryCategory.SEND, "수정된 질문", "수정된 답변", 2, true);

        FaqDto.Response result = faqService.update(1L, req);

        assertThat(result.question()).isEqualTo("수정된 질문");
        assertThat(result.category()).isEqualTo(InquiryCategory.SEND);
        assertThat(result.visible()).isTrue();
        assertThat(result.sortOrder()).isEqualTo(2);
    }
}
