package com.wisecan.unified.dto.cs;

import com.wisecan.unified.domain.cs.Faq;
import com.wisecan.unified.domain.cs.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class FaqDto {

    public record CreateRequest(
        @NotNull(message = "카테고리는 필수입니다")
        InquiryCategory category,

        @NotBlank(message = "질문은 필수입니다")
        @Size(max = 300, message = "질문은 300자 이하입니다")
        String question,

        @NotBlank(message = "답변은 필수입니다")
        String answer,

        int sortOrder,
        boolean visible
    ) {}

    public record UpdateRequest(
        @NotNull InquiryCategory category,
        @NotBlank @Size(max = 300) String question,
        @NotBlank String answer,
        int sortOrder,
        boolean visible
    ) {}

    public record Response(
        Long id,
        InquiryCategory category,
        String question,
        String answer,
        int sortOrder,
        boolean visible,
        LocalDateTime createdAt
    ) {
        public static Response from(Faq faq) {
            return new Response(
                faq.getId(),
                faq.getCategory(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getSortOrder(),
                faq.isVisible(),
                faq.getCreatedAt()
            );
        }
    }
}
