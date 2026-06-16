package com.wisecan.unified.dto.cs;

import com.wisecan.unified.domain.cs.Notice;
import com.wisecan.unified.domain.cs.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class NoticeDto {

    public record CreateRequest(
        @NotNull(message = "공지 유형은 필수입니다")
        NoticeType type,

        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 300, message = "제목은 300자 이하입니다")
        String title,

        @NotBlank(message = "내용은 필수입니다")
        String content,

        boolean pinned,
        boolean visible
    ) {}

    public record UpdateRequest(
        @NotNull NoticeType type,
        @NotBlank @Size(max = 300) String title,
        @NotBlank String content,
        boolean pinned,
        boolean visible
    ) {}

    public record Summary(
        Long id,
        NoticeType type,
        String title,
        boolean pinned,
        boolean visible,
        LocalDateTime createdAt
    ) {
        public static Summary from(Notice notice) {
            return new Summary(
                notice.getId(),
                notice.getType(),
                notice.getTitle(),
                notice.isPinned(),
                notice.isVisible(),
                notice.getCreatedAt()
            );
        }
    }

    public record Detail(
        Long id,
        NoticeType type,
        String title,
        String content,
        boolean pinned,
        boolean visible,
        Long authorAdminId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        public static Detail from(Notice notice) {
            return new Detail(
                notice.getId(),
                notice.getType(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.isVisible(),
                notice.getAuthorAdminId(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
            );
        }
    }
}
