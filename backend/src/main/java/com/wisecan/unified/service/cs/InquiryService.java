package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.Inquiry;
import com.wisecan.unified.domain.cs.InquiryStatus;
import com.wisecan.unified.dto.cs.InquiryDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.cs.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /** 회원이 1:1 문의를 생성한다 */
    @Transactional
    public InquiryDto.Detail create(Long memberId, InquiryDto.CreateRequest request) {
        Inquiry inquiry = Inquiry.builder()
                .memberId(memberId)
                .category(request.category())
                .title(request.title())
                .content(request.content())
                .build();
        return InquiryDto.Detail.from(inquiryRepository.save(inquiry));
    }

    /** 회원 본인 문의 목록 조회 */
    @Transactional(readOnly = true)
    public Page<InquiryDto.Summary> listByMember(Long memberId, Pageable pageable) {
        return inquiryRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(InquiryDto.Summary::from);
    }

    /** 회원 본인 문의 단건 조회 */
    @Transactional(readOnly = true)
    public InquiryDto.Detail detailByMember(Long memberId, Long inquiryId) {
        Inquiry inquiry = findById(inquiryId);
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new EntityNotFoundException("문의를 찾을 수 없습니다.");
        }
        return InquiryDto.Detail.from(inquiry);
    }

    /** 회원이 문의를 종료 처리한다 */
    @Transactional
    public void close(Long memberId, Long inquiryId) {
        Inquiry inquiry = findById(inquiryId);
        if (!inquiry.getMemberId().equals(memberId)) {
            throw new EntityNotFoundException("문의를 찾을 수 없습니다.");
        }
        inquiry.close();
    }

    // ── 관리자 영역 ──────────────────────────────────────────

    /** 관리자: 전체 문의 목록 */
    @Transactional(readOnly = true)
    public Page<InquiryDto.Summary> listAll(Pageable pageable) {
        return inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(InquiryDto.Summary::from);
    }

    /** 관리자: 상태별 문의 목록 */
    @Transactional(readOnly = true)
    public Page<InquiryDto.Summary> listByStatus(InquiryStatus status, Pageable pageable) {
        return inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(InquiryDto.Summary::from);
    }

    /** 관리자: 문의 단건 조회 */
    @Transactional(readOnly = true)
    public InquiryDto.Detail detail(Long inquiryId) {
        return InquiryDto.Detail.from(findById(inquiryId));
    }

    /** 관리자: 처리 중 상태로 전환 */
    @Transactional
    public InquiryDto.Detail markInProgress(Long inquiryId) {
        Inquiry inquiry = findById(inquiryId);
        inquiry.markInProgress();
        return InquiryDto.Detail.from(inquiry);
    }

    /** 관리자: 답변 등록 — 24h SLA 기록 포함 */
    @Transactional
    public InquiryDto.Detail answer(Long inquiryId, Long adminId, InquiryDto.AnswerRequest request) {
        Inquiry inquiry = findById(inquiryId);
        inquiry.answer(request.answerContent(), adminId);
        return InquiryDto.Detail.from(inquiry);
    }

    /** SLA 통계: 지정 기간 내 답변 완료 건의 SLA 충족률 */
    @Transactional(readOnly = true)
    public InquiryDto.SlaStats slaStats(LocalDateTime from, LocalDateTime to) {
        long total = inquiryRepository.countAnsweredBetween(from, to);
        long breached = inquiryRepository.countSlaBreachedBetween(from, to);
        long within = total - breached;
        double rate = total == 0 ? 100.0 : Math.round((within * 1000.0 / total)) / 10.0;
        return new InquiryDto.SlaStats(total, within, breached, rate);
    }

    private Inquiry findById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다. id=" + id));
    }
}
