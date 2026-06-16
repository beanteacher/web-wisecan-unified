package com.wisecan.unified.service;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.BusinessApplication;
import com.wisecan.unified.domain.Company;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.AdminReviewDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.BusinessApplicationRepository;
import com.wisecan.unified.repository.CompanyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 운영자 콘솔 검토 큐 서비스 — W-106
 *
 * §12.1 사업자 전환 심사 / §12.2 발신번호 심사 / §12.6 API 키 검토 큐
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReviewService {

    private final BusinessApplicationRepository businessApplicationRepository;
    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final CallbackRepository callbackRepository;
    private final ApiKeyRepository apiKeyRepository;

    // ── §12.1 사업자 전환 심사 ──────────────────────────────────────

    /**
     * 사업자 전환 신청 심사 큐 목록 조회.
     * SUBMITTED 또는 UNDER_REVIEW 상태인 신청만 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AdminReviewDto.BusinessApplicationSummary> listBusinessApplicationQueue() {
        List<BusinessApplication> apps = businessApplicationRepository.findByStatusIn(
            List.of("SUBMITTED", "UNDER_REVIEW"));

        Set<Long> memberIds = apps.stream()
            .map(BusinessApplication::getMemberId)
            .collect(Collectors.toSet());

        Map<Long, String> emailByMemberId = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, Member::getEmail));

        return apps.stream()
            .map(app -> AdminReviewDto.BusinessApplicationSummary.from(
                app, emailByMemberId.getOrDefault(app.getMemberId(), "-")))
            .toList();
    }

    /**
     * 사업자 전환 신청 상세 조회.
     */
    @Transactional(readOnly = true)
    public AdminReviewDto.BusinessApplicationDetail getBusinessApplicationDetail(Long applicationId) {
        BusinessApplication app = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("BusinessApplication", applicationId));

        Member member = memberRepository.findById(app.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("Member", app.getMemberId()));

        return AdminReviewDto.BusinessApplicationDetail.from(app, member.getEmail());
    }

    /**
     * 심사 시작 — SUBMITTED → UNDER_REVIEW.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.BusinessApplicationDetail startReview(Long applicationId) {
        BusinessApplication app = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("BusinessApplication", applicationId));

        if (!"SUBMITTED".equals(app.getStatus())) {
            throw new IllegalStateException("SUBMITTED 상태인 신청만 심사 시작할 수 있습니다. 현재 상태: " + app.getStatus());
        }

        app.startReview();
        log.info("사업자 전환 심사 시작: applicationId={}", applicationId);

        Member member = memberRepository.findById(app.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("Member", app.getMemberId()));
        return AdminReviewDto.BusinessApplicationDetail.from(app, member.getEmail());
    }

    /**
     * 사업자 전환 승인 — UNDER_REVIEW → APPROVED.
     *
     * 단일 트랜잭션:
     *   1) BusinessApplication.approve()
     *   2) Company INSERT
     *   3) Member.promoteToCompanyMaster(companyId)
     *   4) CompanyRoleLog INSERT (GRANT) — W-103 CompanyService 에 위임하지 않고 직접 처리
     *      (순환 의존 방지 + 트랜잭션 단일화)
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.BusinessApplicationDetail approveBusinessApplication(
        Long applicationId, Long adminId) {

        BusinessApplication app = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("BusinessApplication", applicationId));

        if (!"UNDER_REVIEW".equals(app.getStatus())) {
            throw new IllegalStateException("UNDER_REVIEW 상태인 신청만 승인할 수 있습니다. 현재 상태: " + app.getStatus());
        }

        // 1) 신청 승인
        app.approve(adminId);

        // 2) Company 생성 — billingMode 기본값 PREPAID, status ACTIVE, approvedAt 현재 시각
        Company company = Company.builder()
            .name(app.getCompanyName())
            .bizNumber(app.getBizNumber())
            .billingMode("PREPAID")
            .status("ACTIVE")
            .approvedAt(java.time.LocalDateTime.now())
            .build();
        Company savedCompany = companyRepository.save(company);

        // 3) Member → COMPANY_MASTER 전이
        Member member = memberRepository.findById(app.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("Member", app.getMemberId()));

        if (member.getRole() != MemberRole.MEMBER) {
            throw new IllegalStateException("개인 회원만 사업자 전환 승인이 가능합니다. 현재 역할: " + member.getRole());
        }

        member.promoteToCompanyMaster(savedCompany.getId());

        log.info("사업자 전환 승인: applicationId={}, companyId={}, memberId={}",
            applicationId, savedCompany.getId(), member.getId());

        return AdminReviewDto.BusinessApplicationDetail.from(app, member.getEmail());
    }

    /**
     * 사업자 전환 반려 — UNDER_REVIEW → REJECTED.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.BusinessApplicationDetail rejectBusinessApplication(
        Long applicationId, Long adminId, String reason) {

        BusinessApplication app = businessApplicationRepository.findById(applicationId)
            .orElseThrow(() -> new EntityNotFoundException("BusinessApplication", applicationId));

        if (!"UNDER_REVIEW".equals(app.getStatus()) && !"SUBMITTED".equals(app.getStatus())) {
            throw new IllegalStateException("심사 중인 신청만 반려할 수 있습니다. 현재 상태: " + app.getStatus());
        }

        app.reject(adminId, reason);

        log.info("사업자 전환 반려: applicationId={}, reason={}", applicationId, reason);

        Member member = memberRepository.findById(app.getMemberId())
            .orElseThrow(() -> new EntityNotFoundException("Member", app.getMemberId()));
        return AdminReviewDto.BusinessApplicationDetail.from(app, member.getEmail());
    }

    // ── §12.2 발신번호 심사 ─────────────────────────────────────────

    /**
     * 발신번호 심사 큐 목록 조회.
     * SUBMITTED 또는 UNDER_REVIEW 상태인 발신번호만 반환한다.
     */
    @Transactional(readOnly = true)
    public List<AdminReviewDto.CallbackReviewSummary> listCallbackQueue() {
        List<Callback> callbacks = callbackRepository.findByStatusIn(
            List.of(CallbackStatus.SUBMITTED, CallbackStatus.UNDER_REVIEW));

        Set<Long> memberIds = callbacks.stream()
            .filter(cb -> cb.getMemberId() != null)
            .map(Callback::getMemberId)
            .collect(Collectors.toSet());

        Map<Long, String> emailByMemberId = memberRepository.findAllById(memberIds).stream()
            .collect(Collectors.toMap(Member::getId, Member::getEmail));

        return callbacks.stream()
            .map(cb -> AdminReviewDto.CallbackReviewSummary.from(
                cb, cb.getMemberId() != null
                    ? emailByMemberId.getOrDefault(cb.getMemberId(), "-")
                    : "(회사 직접 등록)"))
            .toList();
    }

    /**
     * 발신번호 승인 — UNDER_REVIEW → REGISTERED.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.CallbackReviewSummary approveCallback(Long callbackId) {
        Callback cb = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("Callback", callbackId));

        if (cb.getStatus() != CallbackStatus.SUBMITTED
            && cb.getStatus() != CallbackStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 대기 중인 발신번호만 승인할 수 있습니다. 현재 상태: " + cb.getStatus());
        }

        cb.approve();

        log.info("발신번호 승인: callbackId={}, phoneNumber={}", callbackId, cb.getPhoneNumber());

        String email = cb.getMemberId() != null
            ? memberRepository.findById(cb.getMemberId()).map(Member::getEmail).orElse("-")
            : "(회사 직접 등록)";
        return AdminReviewDto.CallbackReviewSummary.from(cb, email);
    }

    /**
     * 발신번호 반려 — UNDER_REVIEW → REJECTED.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.CallbackReviewSummary rejectCallback(Long callbackId, String reason) {
        Callback cb = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("Callback", callbackId));

        if (cb.getStatus() != CallbackStatus.SUBMITTED
            && cb.getStatus() != CallbackStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 대기 중인 발신번호만 반려할 수 있습니다. 현재 상태: " + cb.getStatus());
        }

        cb.reject(reason);

        log.info("발신번호 반려: callbackId={}, reason={}", callbackId, reason);

        String email = cb.getMemberId() != null
            ? memberRepository.findById(cb.getMemberId()).map(Member::getEmail).orElse("-")
            : "(회사 직접 등록)";
        return AdminReviewDto.CallbackReviewSummary.from(cb, email);
    }

    // ── §12.6 API 키 검토 큐 ───────────────────────────────────────

    /**
     * 운영 키(PRODUCTION) 검토 큐 목록 조회.
     * ApiKeyStatus.PENDING_REVIEW 상태인 키가 대상이다.
     * W-105에서 PRODUCTION 키 발급 시 PENDING_REVIEW 상태로 생성됨.
     */
    @Transactional(readOnly = true)
    public List<AdminReviewDto.ApiKeyReviewSummary> listApiKeyQueue() {
        List<ApiKey> keys = apiKeyRepository.findByStatus(ApiKeyStatus.PENDING_REVIEW);

        return keys.stream()
            .map(key -> AdminReviewDto.ApiKeyReviewSummary.from(key, key.getMember().getEmail()))
            .toList();
    }

    /**
     * 운영 API 키 승인 — PENDING_REVIEW → ACTIVE.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.ApiKeyReviewSummary approveApiKey(Long apiKeyId) {
        ApiKey key = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new EntityNotFoundException("ApiKey", apiKeyId));

        if (key.getStatus() != ApiKeyStatus.PENDING_REVIEW) {
            throw new IllegalStateException("검토 대기 중인 API 키만 승인할 수 있습니다. 현재 상태: " + key.getStatus());
        }

        key.activate();

        log.info("API 키 승인: apiKeyId={}, memberId={}", apiKeyId, key.getMember().getId());

        return AdminReviewDto.ApiKeyReviewSummary.from(key, key.getMember().getEmail());
    }

    /**
     * 운영 API 키 반려 — PENDING_REVIEW → REVOKED.
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminReviewDto.ApiKeyReviewSummary rejectApiKey(Long apiKeyId, String reason) {
        ApiKey key = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new EntityNotFoundException("ApiKey", apiKeyId));

        if (key.getStatus() != ApiKeyStatus.PENDING_REVIEW) {
            throw new IllegalStateException("검토 대기 중인 API 키만 반려할 수 있습니다. 현재 상태: " + key.getStatus());
        }

        key.revoke();

        log.info("API 키 반려: apiKeyId={}, reason={}", apiKeyId, reason);

        return AdminReviewDto.ApiKeyReviewSummary.from(key, key.getMember().getEmail());
    }
}
