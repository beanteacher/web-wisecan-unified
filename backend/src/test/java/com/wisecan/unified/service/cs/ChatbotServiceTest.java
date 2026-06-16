package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.dto.cs.ChatbotDto;
import com.wisecan.unified.dto.cs.FaqDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private FaqService faqService;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    @DisplayName("챗봇 질의 — FAQ 매칭 성공 시 matched=true와 답변 반환")
    void query_matched() {
        FaqDto.Response faq = new FaqDto.Response(
                1L, InquiryCategory.BILLING, "충전은 어떻게 하나요?",
                "대시보드에서 충전 가능합니다.", 1, true, null);
        given(faqService.searchByKeyword(anyString())).willReturn(List.of(faq));

        ChatbotDto.QueryResponse result = chatbotService.query("충전 방법");

        assertThat(result.matched()).isTrue();
        assertThat(result.answer()).isEqualTo("대시보드에서 충전 가능합니다.");
        assertThat(result.matchedQuestion()).isEqualTo("충전은 어떻게 하나요?");
        assertThat(result.fallbackMessage()).isNull();
    }

    @Test
    @DisplayName("챗봇 질의 — FAQ 미매칭 시 matched=false와 fallback 메시지")
    void query_notMatched_fallback() {
        given(faqService.searchByKeyword(anyString())).willReturn(List.of());

        ChatbotDto.QueryResponse result = chatbotService.query("알 수 없는 질문입니다");

        assertThat(result.matched()).isFalse();
        assertThat(result.answer()).isNull();
        assertThat(result.fallbackMessage()).contains("1:1 문의");
    }

    @Test
    @DisplayName("챗봇 질의 — 특수문자 제거 후 키워드 정규화")
    void query_specialCharsRemoved() {
        given(faqService.searchByKeyword("충전")).willReturn(List.of());

        ChatbotDto.QueryResponse result = chatbotService.query("충전!!?? 어떻게");

        // 특수문자 제거 후 첫 토큰 "충전"으로 재시도했을 때 empty → fallback
        assertThat(result.matched()).isFalse();
    }
}
