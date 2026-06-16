package com.wisecan.unified.controller.billing;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.billing.RefundDto;
import com.wisecan.unified.service.billing.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 환불 컨트롤러 — W-404.
 *
 * POST   /billing/refund              환불 신청
 * GET    /billing/refund              내 환불 목록 조회
 * DELETE /billing/refund/{refundId}   환불 취소 (PENDING 상태만)
 */
@RestController
@RequestMapping("/billing/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /** 환불 신청 */
    @PostMapping
    public ResponseEntity<ApiResponse<RefundDto.Response>> requestRefund(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid RefundDto.CreateRequest request) {
        RefundDto.Response response = refundService.requestRefund(member.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 내 환불 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundDto.Response>>> listRefunds(
            @AuthenticationPrincipal Member member) {
        List<RefundDto.Response> responses = refundService.listRefunds(member.getId());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /** 환불 취소 (회원 직접, PENDING 상태만) */
    @DeleteMapping("/{refundId}")
    public ResponseEntity<ApiResponse<Boolean>> cancelRefund(
            @AuthenticationPrincipal Member member,
            @PathVariable Long refundId) {
        refundService.cancelRefund(member.getId(), refundId);
        return ResponseEntity.ok(ApiResponse.success(true));
    }
}
