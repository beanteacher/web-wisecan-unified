package com.wisecan.unified.dto.cs;

import jakarta.validation.constraints.NotBlank;

public class ChatbotDto {

    public record QueryRequest(
        @NotBlank(message = "질문은 필수입니다")
        String question
    ) {}

    public record QueryResponse(
        /** 매칭된 FAQ 질문 (없으면 null) */
        String matchedQuestion,
        /** FAQ 기반 Stub 답변 */
        String answer,
        /** FAQ 매칭 여부. false 이면 1:1 문의 유도 */
        boolean matched,
        /** 1:1 문의 유도 메시지 */
        String fallbackMessage
    ) {
        public static QueryResponse matched(String matchedQuestion, String answer) {
            return new QueryResponse(matchedQuestion, answer, true, null);
        }

        public static QueryResponse fallback() {
            return new QueryResponse(
                null,
                null,
                false,
                "죄송합니다. 해당 질문에 대한 답변을 찾지 못했습니다. 1:1 문의를 이용해 주세요."
            );
        }
    }
}
