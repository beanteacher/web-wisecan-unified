package com.wisecan.unified.controller;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.CompanyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final MemberRepository memberRepository;

    // ── 회사 정보 조회 ──────────────────────────────────────────────────────────

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<CompanyDto.CompanyInfoResponse>> getCompanyInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        Member member = resolveCurrentMaster(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                companyService.getCompanyInfo(member.getCompanyId())));
    }

    // ── 하위 계정 목록 조회 ─────────────────────────────────────────────────────

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<CompanyDto.SubAccountListResponse>> listSubAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        Member member = resolveCurrentMaster(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                companyService.listSubAccounts(member.getCompanyId())));
    }

    // ── 하위 계정 직접 생성 ─────────────────────────────────────────────────────

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<CompanyDto.SubAccountResponse>> createSubAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompanyDto.CreateSubAccountRequest request) {
        Member member = resolveCurrentMaster(userDetails);
        CompanyDto.SubAccountResponse response =
                companyService.createSubAccount(member.getCompanyId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── 하위 계정 비활성화 ──────────────────────────────────────────────────────

    @PatchMapping("/members/{subAccountId}/disable")
    public ResponseEntity<ApiResponse<CompanyDto.SubAccountResponse>> disableSubAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subAccountId) {
        Member member = resolveCurrentMaster(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                companyService.disableSubAccount(member.getCompanyId(), subAccountId)));
    }

    // ── 하위 계정 삭제 ──────────────────────────────────────────────────────────

    @DeleteMapping("/members/{subAccountId}")
    public ResponseEntity<ApiResponse<Void>> deleteSubAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long subAccountId) {
        Member member = resolveCurrentMaster(userDetails);
        companyService.deleteSubAccount(member.getCompanyId(), subAccountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 초대 링크 발행 ──────────────────────────────────────────────────────────

    @PostMapping("/invitations")
    public ResponseEntity<ApiResponse<CompanyDto.InvitationResponse>> createInvitation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompanyDto.CreateInvitationRequest request) {
        Member member = resolveCurrentMaster(userDetails);
        CompanyDto.InvitationResponse response =
                companyService.createInvitation(member.getCompanyId(), member.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── 초대 수락 (비인증 공개 엔드포인트) ─────────────────────────────────────

    @PostMapping("/invitations/accept")
    public ResponseEntity<ApiResponse<CompanyDto.SubAccountResponse>> acceptInvitation(
            @RequestBody @Valid CompanyDto.AcceptInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(companyService.acceptInvitation(request)));
    }

    // ── 마스터 권한 이관 ────────────────────────────────────────────────────────

    @PostMapping("/master-roles/transfer")
    public ResponseEntity<ApiResponse<Void>> transferMaster(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CompanyDto.TransferMasterRequest request) {
        Member member = resolveCurrentMaster(userDetails);
        companyService.transferMaster(member.getCompanyId(), member.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 마스터 권한 회수 (운영자 또는 본인 요청) ────────────────────────────────

    @PostMapping("/master-roles/revoke/{targetMemberId}")
    public ResponseEntity<ApiResponse<Void>> revokeMaster(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long targetMemberId) {
        Member actor = resolveCurrentMaster(userDetails);
        companyService.revokeMaster(actor.getCompanyId(), targetMemberId, actor.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 권한 이력 조회 ──────────────────────────────────────────────────────────

    @GetMapping("/master-roles/logs")
    public ResponseEntity<ApiResponse<List<CompanyDto.RoleLogResponse>>> getRoleLogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        Member member = resolveCurrentMaster(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                companyService.getRoleLogs(member.getCompanyId())));
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────────────────

    private Member resolveCurrentMaster(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Member", 0L));
        if (member.getRole() != MemberRole.COMPANY_MASTER) {
            throw new IllegalArgumentException("회사 마스터 권한이 필요합니다.");
        }
        if (member.getCompanyId() == null) {
            throw new IllegalArgumentException("소속 회사가 없습니다.");
        }
        return member;
    }
}
