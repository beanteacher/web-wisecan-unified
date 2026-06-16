package com.wisecan.unified.domain.cs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class InquiryTest {

    @Test
    @DisplayName("신규 문의 생성 시 OPEN 상태")
    void newInquiry_isOpen() {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L)
                .category(InquiryCategory.BILLING)
                .title("충전 오류")
                .content("내용")
                .build();
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.OPEN);
    }

    @Test
    @DisplayName("answer() 호출 시 ANSWERED 전환 및 answeredAt 기록")
    void answer_changesStatusAndRecordsTime() {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.TECHNICAL)
                .title("오류").content("내용").build();

        inquiry.answer("해결했습니다.", 99L);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getAnsweredAt()).isNotNull();
        assertThat(inquiry.getAnsweredByAdminId()).isEqualTo(99L);
        assertThat(inquiry.getAnswerContent()).isEqualTo("해결했습니다.");
    }

    @Test
    @DisplayName("close() 호출 시 CLOSED 전환")
    void close_changesStatusToClosed() {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.ETC)
                .title("종료").content("내용").build();

        inquiry.close();

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CLOSED);
    }

    @Test
    @DisplayName("24h 이내 답변 시 SLA 미초과")
    void sla_within24h_notBreached() throws Exception {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.ACCOUNT)
                .title("질문").content("내용").build();

        // createdAt 강제 설정 (reflection)
        Field createdAtField = Inquiry.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(inquiry, LocalDateTime.now().minusHours(20));

        inquiry.answer("답변", 1L);

        assertThat(inquiry.isSlaBreached()).isFalse();
    }

    @Test
    @DisplayName("24h 초과 답변 시 SLA 초과")
    void sla_over24h_breached() throws Exception {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.ACCOUNT)
                .title("질문").content("내용").build();

        Field createdAtField = Inquiry.class.getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(inquiry, LocalDateTime.now().minusHours(25));

        inquiry.answer("늦은 답변", 1L);

        assertThat(inquiry.isSlaBreached()).isTrue();
    }

    @Test
    @DisplayName("미답변 문의의 getAnswerMinutes()는 -1")
    void unanswered_answerMinutes_isMinusOne() {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.SEND)
                .title("발송 오류").content("내용").build();

        assertThat(inquiry.getAnswerMinutes()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("OPEN 상태에서 markInProgress() 시 IN_PROGRESS 전환")
    void markInProgress_fromOpen() {
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L).category(InquiryCategory.ETC)
                .title("제목").content("내용").build();

        inquiry.markInProgress();

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.IN_PROGRESS);
    }
}
