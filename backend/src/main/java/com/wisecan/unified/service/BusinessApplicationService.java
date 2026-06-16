package com.wisecan.unified.service;

import com.wisecan.unified.domain.BusinessApplication;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.dto.BusinessApplicationDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.BusinessApplicationRepository;
import com.wisecan.unified.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BusinessApplicationService {

    private final BusinessApplicationRepository businessApplicationRepository;
    private final MemberRepository memberRepository;

    public BusinessApplicationDto.StatusResponse submit(String email,
                                                        BusinessApplicationDto.SubmitRequest request) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        boolean alreadyPending = businessApplicationRepository.existsByMemberIdAndStatusIn(
            member.getId(), List.of("SUBMITTED", "UNDER_REVIEW")
        );
        if (alreadyPending) {
            throw new IllegalStateException("이미 심사 중인 사업자 전환 신청이 있습니다.");
        }

        BusinessApplication application = BusinessApplication.builder()
            .memberId(member.getId())
            .status("SUBMITTED")
            .bizNumber(request.bizNumber())
            .corpNumber(request.corpNumber())
            .companyName(request.companyName())
            .ceoName(request.ceoName())
            .ceoPhone(request.ceoPhone())
            .build();

        BusinessApplication saved = businessApplicationRepository.save(application);

        return new BusinessApplicationDto.StatusResponse(
            saved.getId(),
            saved.getStatus(),
            saved.getCompanyName(),
            saved.getBizNumber(),
            null
        );
    }

    @Transactional(readOnly = true)
    public BusinessApplicationDto.StatusResponse getStatus(String email) {
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        BusinessApplication application = businessApplicationRepository
            .findByMemberIdAndStatus(member.getId(), "SUBMITTED")
            .or(() -> businessApplicationRepository.findByMemberIdAndStatus(member.getId(), "UNDER_REVIEW"))
            .or(() -> businessApplicationRepository.findByMemberIdAndStatus(member.getId(), "APPROVED"))
            .or(() -> businessApplicationRepository.findByMemberIdAndStatus(member.getId(), "REJECTED"))
            .orElseThrow(() -> new RuntimeException("사업자 전환 신청 내역이 없습니다."));

        return new BusinessApplicationDto.StatusResponse(
            application.getId(),
            application.getStatus(),
            application.getCompanyName(),
            application.getBizNumber(),
            application.getRejectReason()
        );
    }

    // ── 운영자 심사 (§12.1 — W-106 운영자 콘솔 검토 큐에서 호출) ──

    /**
     * 운영자 심사 큐 목록 — SUBMITTED + UNDER_REVIEW 상태 전체.
     * W-106 /admin/review/business 진입점.
     */
    @Transactional(readOnly = true)
    public List<BusinessApplicationDto.StatusResponse> listPendingReview() {
        return businessApplicationRepository
            .findByStatusIn(List.of("SUBMITTED", "UNDER_REVIEW"))
            .stream()
            .map(a -> new BusinessApplicationDto.StatusResponse(
                a.getId(), a.getStatus(), a.getCompanyName(), a.getBizNumber(), a.getRejectReason()))
            .collect(Collectors.toList());
    }

    /**
     * 운영자 검토 시작 — SUBMITTED → UNDER_REVIEW.
     */
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long applicationId, Long actorOperatorId) {
        BusinessApplication application = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("사업자 전환 신청을 찾을 수 없습니다: " + applicationId));

        if (!"SUBMITTED".equals(application.getStatus())) {
            throw new IllegalStateException("SUBMITTED 상태가 아닙니다: " + application.getStatus());
        }
        application.startReview();
        log.info("사업자 전환 심사 시작: applicationId={}, operatorId={}", applicationId, actorOperatorId);
    }

    /**
     * 운영자 승인 — UNDER_REVIEW → APPROVED + 회원 COMPANY_MASTER 승격.
     * §12.1 정상 흐름: 승인 시 기존 계정에 회사 마스터 권한 부여.
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long applicationId, Long actorOperatorId) {
        BusinessApplication application = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("사업자 전환 신청을 찾을 수 없습니다: " + applicationId));

        if (!"SUBMITTED".equals(application.getStatus()) && !"UNDER_REVIEW".equals(application.getStatus())) {
            throw new IllegalStateException("심사 대기 또는 검토 중 상태가 아닙니다: " + application.getStatus());
        }

        application.approve(actorOperatorId);

        // 회원 COMPANY_MASTER 승격 (companyId는 추후 Company 도메인 연동 시 갱신)
        Member member = memberRepository.findById(application.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + application.getMemberId()));
        member.promoteToCompanyMaster(null);

        log.info("사업자 전환 승인: applicationId={}, memberId={}, operatorId={}",
            applicationId, application.getMemberId(), actorOperatorId);
    }

    /**
     * 운영자 반려 — SUBMITTED/UNDER_REVIEW → REJECTED + 사유 기록.
     */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long applicationId, Long actorOperatorId, String reason) {
        BusinessApplication application = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("사업자 전환 신청을 찾을 수 없습니다: " + applicationId));

        if (!"SUBMITTED".equals(application.getStatus()) && !"UNDER_REVIEW".equals(application.getStatus())) {
            throw new IllegalStateException("심사 대기 또는 검토 중 상태가 아닙니다: " + application.getStatus());
        }

        application.reject(actorOperatorId, reason);
        log.info("사업자 전환 반려: applicationId={}, operatorId={}, reason={}",
            applicationId, actorOperatorId, reason);
    }
}
