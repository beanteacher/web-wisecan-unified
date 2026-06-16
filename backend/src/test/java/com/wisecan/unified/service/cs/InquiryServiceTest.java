package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.Inquiry;
import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.domain.cs.InquiryStatus;
import com.wisecan.unified.dto.cs.InquiryDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.cs.InquiryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("문의 생성 — 정상 저장 후 OPEN 상태 반환")
    void create_success() {
        // given
        InquiryDto.CreateRequest request = new InquiryDto.CreateRequest(
                InquiryCategory.BILLING, "충전 오류", "충전이 안 됩니다.");
        Inquiry saved = Inquiry.builder()
                .memberId(1L)
                .category(InquiryCategory.BILLING)
                .title("충전 오류")
                .content("충전이 안 됩니다.")
                .build();
        given(inquiryRepository.save(any(Inquiry.class))).willReturn(saved);

        // when
        InquiryDto.Detail result = inquiryService.create(1L, request);

        // then
        assertThat(result.status()).isEqualTo(InquiryStatus.OPEN);
        assertThat(result.title()).isEqualTo("충전 오류");
        then(inquiryRepository).should().save(any(Inquiry.class));
    }

    @Test
    @DisplayName("회원 본인 문의 단건 조회 — 타인 접근 시 예외")
    void detailByMember_wrongMember_throws() {
        // given
        Inquiry inquiry = Inquiry.builder()
                .memberId(2L)
                .category(InquiryCategory.ETC)
                .title("제목")
                .content("내용")
                .build();
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        // when / then
        assertThatThrownBy(() -> inquiryService.detailByMember(1L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("관리자 답변 등록 — ANSWERED 상태 전환 및 SLA 시각 기록")
    void answer_setsAnsweredAtAndStatus() {
        // given
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L)
                .category(InquiryCategory.TECHNICAL)
                .title("API 오류")
                .content("API 호출이 실패합니다.")
                .build();
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        InquiryDto.AnswerRequest answerReq = new InquiryDto.AnswerRequest("확인 후 수정했습니다.");

        // when
        InquiryDto.Detail result = inquiryService.answer(1L, 99L, answerReq);

        // then
        assertThat(result.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(result.answerContent()).isEqualTo("확인 후 수정했습니다.");
        assertThat(result.answeredAt()).isNotNull();
        assertThat(result.answeredByAdminId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("SLA 통계 — 전체 0건이면 rate 100%")
    void slaStats_noAnswered_returns100() {
        // given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        given(inquiryRepository.countAnsweredBetween(from, to)).willReturn(0L);
        given(inquiryRepository.countSlaBreachedBetween(from, to)).willReturn(0L);

        // when
        InquiryDto.SlaStats stats = inquiryService.slaStats(from, to);

        // then
        assertThat(stats.totalAnswered()).isZero();
        assertThat(stats.slaRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("SLA 통계 — 10건 중 2건 초과 시 rate 80%")
    void slaStats_2breached_returns80percent() {
        // given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        given(inquiryRepository.countAnsweredBetween(from, to)).willReturn(10L);
        given(inquiryRepository.countSlaBreachedBetween(from, to)).willReturn(2L);

        // when
        InquiryDto.SlaStats stats = inquiryService.slaStats(from, to);

        // then
        assertThat(stats.totalAnswered()).isEqualTo(10L);
        assertThat(stats.withinSla()).isEqualTo(8L);
        assertThat(stats.breachedSla()).isEqualTo(2L);
        assertThat(stats.slaRate()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("문의 종료 — 본인이 close 처리하면 CLOSED 상태")
    void close_success() {
        // given
        Inquiry inquiry = Inquiry.builder()
                .memberId(1L)
                .category(InquiryCategory.ETC)
                .title("종료 테스트")
                .content("내용")
                .build();
        given(inquiryRepository.findById(1L)).willReturn(Optional.of(inquiry));

        // when
        inquiryService.close(1L, 1L);

        // then
        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.CLOSED);
    }

    @Test
    @DisplayName("존재하지 않는 문의 조회 시 EntityNotFoundException")
    void detail_notFound_throws() {
        given(inquiryRepository.findById(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> inquiryService.detail(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
