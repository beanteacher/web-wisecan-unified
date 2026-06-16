package com.wisecan.unified.dto.cs;

import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.domain.cs.InquiryStatus;
import com.wisecan.unified.domain.cs.Inquiry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class InquiryDto {

    public record CreateRequest(
        @NotNull(message = "카테고리는 필수입니다")
        InquiryCategory category,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자 이하입니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        String content
    ) {}

    public record AnswerRequest(
        @NotBlank(message = "답변 내용은 필수입니다")
        String answerContent
    ) {}

    public record Summary(
        Long id,
        InquiryCategory category,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt,
        /** 답변 소요 시간(분). 미답변 -1 */
        long answerMinutes
    ) {
        public static Summary from(Inquiry inquiry) {
            return new Summary(
                inquiry.getId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getAnsweredAt(),
                inquiry.getAnswerMinutes()
            );
        }
    }

    public record Detail(
        Long id,
        Long memberId,
        InquiryCategory category,
        String title,
        String content,
        InquiryStatus status,
        String answerContent,
        Long answeredByAdminId,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long answerMinutes,
        boolean slaBreached
    ) {
        public static Detail from(Inquiry inquiry) {
            return new Detail(
                inquiry.getId(),
                inquiry.getMemberId(),
                inquiry.getCategory(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAnswerContent(),
                inquiry.getAnsweredByAdminId(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt(),
                inquiry.getAnswerMinutes(),
                inquiry.isSlaBreached()
            );
        }
    }

    /** SLA 집계 응답 */
    public record SlaStats(
        long totalAnswered,
        long withinSla,
        long breachedSla,
        double slaRate
    ) {}
}
