package com.wisecan.unified.controller;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.dto.AdminReviewDto;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.AdminReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 운영자 콘솔 검토 큐 API — W-106
 *
 * §12.1 /api/v1/admin/review/business   — 사업자 전환 심사
 * §12.2 /api/v1/admin/review/callbacks  — 발신번호 심사
 * §12.6 /api/v1/admin/review/api-keys   — API 키 운영 전환 검토 큐
 *
 * 모든 엔드포인트는 ADMIN 또는 SUPER_ADMIN 역할이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/admin/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;
    private final MemberRepository memberRepository;

    // ── §12.1 사업자 전환 심사 ──────────────────────────────────────

    /** 사업자 전환 심사 큐 목록 조회 */
    @GetMapping("/business")
    public ResponseEntity<ApiResponse<List<AdminReviewDto.BusinessApplicationSummary>>> listBusinessQueue(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.listBusinessApplicationQueue()));
    }

    /** 사업자 전환 신청 상세 조회 */
    @GetMapping("/business/{applicationId}")
    public ResponseEntity<ApiResponse<AdminReviewDto.BusinessApplicationDetail>> getBusinessDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long applicationId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.getBusinessApplicationDetail(applicationId)));
    }

    /** 심사 시작 — SUBMITTED → UNDER_REVIEW */
    @PostMapping("/business/{applicationId}/start")
    public ResponseEntity<ApiResponse<AdminReviewDto.BusinessApplicationDetail>> startReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long applicationId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.startReview(applicationId)));
    }

    /** 사업자 전환 승인 — UNDER_REVIEW → APPROVED + Company 생성 + Member 전이 */
    @PostMapping("/business/{applicationId}/approve")
    public ResponseEntity<ApiResponse<AdminReviewDto.BusinessApplicationDetail>> approveBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long applicationId) {
        Long adminId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.approveBusinessApplication(applicationId, adminId)));
    }

    /** 사업자 전환 반려 — UNDER_REVIEW → REJECTED */
    @PostMapping("/business/{applicationId}/reject")
    public ResponseEntity<ApiResponse<AdminReviewDto.BusinessApplicationDetail>> rejectBusiness(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long applicationId,
            @RequestBody @Valid AdminReviewDto.RejectBusinessRequest request) {
        Long adminId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.rejectBusinessApplication(applicationId, adminId, request.reason())));
    }

    // ── §12.2 발신번호 심사 ─────────────────────────────────────────

    /** 발신번호 심사 큐 목록 조회 */
    @GetMapping("/callbacks")
    public ResponseEntity<ApiResponse<List<AdminReviewDto.CallbackReviewSummary>>> listCallbackQueue(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.listCallbackQueue()));
    }

    /** 발신번호 승인 */
    @PostMapping("/callbacks/{callbackId}/approve")
    public ResponseEntity<ApiResponse<AdminReviewDto.CallbackReviewSummary>> approveCallback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long callbackId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.approveCallback(callbackId)));
    }

    /** 발신번호 반려 */
    @PostMapping("/callbacks/{callbackId}/reject")
    public ResponseEntity<ApiResponse<AdminReviewDto.CallbackReviewSummary>> rejectCallback(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long callbackId,
            @RequestBody @Valid AdminReviewDto.RejectCallbackRequest request) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.rejectCallback(callbackId, request.reason())));
    }

    // ── §12.6 API 키 검토 큐 ───────────────────────────────────────

    /** 운영 API 키 검토 큐 목록 조회 */
    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<AdminReviewDto.ApiKeyReviewSummary>>> listApiKeyQueue(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.listApiKeyQueue()));
    }

    /** 운영 API 키 승인 — PENDING_REVIEW → ACTIVE */
    @PostMapping("/api-keys/{apiKeyId}/approve")
    public ResponseEntity<ApiResponse<AdminReviewDto.ApiKeyReviewSummary>> approveApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long apiKeyId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.approveApiKey(apiKeyId)));
    }

    /** 운영 API 키 반려 — PENDING_REVIEW → REVOKED */
    @PostMapping("/api-keys/{apiKeyId}/reject")
    public ResponseEntity<ApiResponse<AdminReviewDto.ApiKeyReviewSummary>> rejectApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long apiKeyId,
            @RequestBody @Valid AdminReviewDto.RejectApiKeyRequest request) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminReviewService.rejectApiKey(apiKeyId, request.reason())));
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────

    /**
     * JWT principal로부터 운영자 Member ID를 추출한다.
     * ADMIN 또는 SUPER_ADMIN 역할이 아니면 예외를 던진다.
     */
    private Long resolveAdminId(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Member", 0L));

        MemberRole role = member.getRole();
        if (role != MemberRole.ADMIN && role != MemberRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("운영자 권한이 필요합니다.");
        }
        return member.getId();
    }
}
