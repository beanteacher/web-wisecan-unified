package com.wisecan.unified.controller.admin;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.admin.AdminMemberControlDto;
import com.wisecan.unified.service.admin.AdminMemberControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 운영자 회원 통제 API — W-501, §12.3.
 *
 * POST /api/v1/admin/members/{memberId}/suspend    강제 정지 (SUSPENDED + 발신번호·키 연쇄)
 * POST /api/v1/admin/members/{memberId}/terminate  강제 해지 (TERMINATED + 발신번호·키 연쇄)
 * POST /api/v1/admin/members/{memberId}/unsuspend  정지 해제 (ACTIVE 복귀)
 * GET  /api/v1/admin/members/{memberId}/audit-log  통제 감사 로그 조회
 */
@RestController
@RequestMapping("/api/v1/admin/members")
@RequiredArgsConstructor
public class AdminMemberControlController {

    private final AdminMemberControlService adminMemberControlService;
    private final AdminAuthHelper adminAuthHelper;

    /** 강제 정지 */
    @PostMapping("/{memberId}/suspend")
    public ResponseEntity<ApiResponse<AdminMemberControlDto.MemberStatusResponse>> suspend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId,
            @RequestBody @Valid AdminMemberControlDto.SuspendRequest request) {
        Long operatorId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberControlService.suspend(memberId, operatorId, request.reason())));
    }

    /** 강제 해지 */
    @PostMapping("/{memberId}/terminate")
    public ResponseEntity<ApiResponse<AdminMemberControlDto.MemberStatusResponse>> terminate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId,
            @RequestBody @Valid AdminMemberControlDto.TerminateRequest request) {
        Long operatorId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberControlService.terminate(memberId, operatorId, request.reason())));
    }

    /** 정지 해제 */
    @PostMapping("/{memberId}/unsuspend")
    public ResponseEntity<ApiResponse<AdminMemberControlDto.MemberStatusResponse>> unsuspend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId,
            @RequestBody @Valid AdminMemberControlDto.UnsuspendRequest request) {
        Long operatorId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberControlService.unsuspend(memberId, operatorId, request.reason())));
    }

    /** 통제 감사 로그 조회 */
    @GetMapping("/{memberId}/audit-log")
    public ResponseEntity<ApiResponse<List<AdminMemberControlDto.ControlAuditEntry>>> auditLog(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminMemberControlService.listAuditLog(memberId)));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────

    private Long resolveAdminId(UserDetails userDetails) {
        return adminAuthHelper.resolveAdminId(userDetails);
    }
}
