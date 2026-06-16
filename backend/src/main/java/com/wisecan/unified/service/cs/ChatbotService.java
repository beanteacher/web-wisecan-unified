package com.wisecan.unified.service.cs;

import com.wisecan.unified.dto.cs.ChatbotDto;
import com.wisecan.unified.dto.cs.FaqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FAQ 기반 챗봇 Stub 서비스.
 * 질문 키워드로 FAQ를 검색하여 가장 적합한 답변을 반환한다.
 * 향후 LLM 연동 시 이 클래스를 교체 또는 확장한다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final FaqService faqService;

    /**
     * 사용자 질문에서 키워드를 추출해 FAQ를 검색한다.
     * 매칭되면 첫 번째 FAQ 답변 반환, 없으면 fallback 메시지.
     */
    @Transactional(readOnly = true)
    public ChatbotDto.QueryResponse query(String question) {
        String keyword = extractKeyword(question);
        List<FaqDto.Response> results = faqService.searchByKeyword(keyword);

        if (results.isEmpty()) {
            // 단어 단위 재시도 (공백 분리 첫 번째 토큰)
            String[] tokens = question.trim().split("\\s+");
            for (String token : tokens) {
                if (token.length() >= 2) {
                    results = faqService.searchByKeyword(token);
                    if (!results.isEmpty()) break;
                }
            }
        }

        if (results.isEmpty()) {
            return ChatbotDto.QueryResponse.fallback();
        }

        FaqDto.Response best = results.get(0);
        return ChatbotDto.QueryResponse.matched(best.question(), best.answer());
    }

    /** 키워드 정규화: 특수문자 제거, 앞뒤 공백 trim */
    private String extractKeyword(String question) {
        return question.replaceAll("[^가-힣a-zA-Z0-9\\s]", "").trim();
    }
}
