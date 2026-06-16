package com.wisecan.unified.controller.admin;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.admin.AdminBillingDto;
import com.wisecan.unified.service.admin.AdminBillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 운영자 재무 관리 API — W-501, §12.5.
 *
 * GET  /api/v1/admin/billing/refunds                   전체 환불 목록 조회
 * POST /api/v1/admin/billing/cash-adjust               캐시 강제 적립/차감
 * GET  /api/v1/admin/billing/settlement                정산 보고서 조회
 *
 * 환불 승인·반려는 AdminRefundController (W-404) 참고.
 */
@RestController
@RequestMapping("/api/v1/admin/billing")
@RequiredArgsConstructor
public class AdminBillingController {

    private final AdminBillingService adminBillingService;
    private final AdminAuthHelper adminAuthHelper;

    /** 전체 환불 목록 조회 */
    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<List<AdminBillingDto.RefundSummary>>> listRefunds(
            @AuthenticationPrincipal UserDetails userDetails) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminBillingService.listAllRefunds()));
    }

    /** 캐시 강제 적립/차감 (amount > 0 = 적립, amount < 0 = 차감) */
    @PostMapping("/cash-adjust")
    public ResponseEntity<ApiResponse<AdminBillingDto.CashAdjustResponse>> cashAdjust(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid AdminBillingDto.CashAdjustRequest request) {
        Long operatorId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminBillingService.adjustCash(operatorId, request)));
    }

    /** 정산 보고서 조회 */
    @GetMapping("/settlement")
    public ResponseEntity<ApiResponse<AdminBillingDto.SettlementReport>> settlement(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime periodEnd) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                adminBillingService.generateSettlementReport(periodStart, periodEnd)));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────

    private Long resolveAdminId(UserDetails userDetails) {
        return adminAuthHelper.resolveAdminId(userDetails);
    }
}
