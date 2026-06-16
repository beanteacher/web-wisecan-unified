package com.wisecan.unified.controller.admin;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.billing.RefundDto;
import com.wisecan.unified.service.billing.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 환불 처리 컨트롤러 — W-404, 02_FEATURE_SPEC §12.5.
 *
 * PUT /admin/billing/refund/{refundId}/approve  운영자 승인
 * PUT /admin/billing/refund/{refundId}/reject   운영자 반려
 */
@RestController
@RequestMapping("/admin/billing/refund")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    /** 환불 승인 — 잔액 차감 + 현금영수증 마이너스 정정(해당 시) 자동 처리 */
    @PutMapping("/{refundId}/approve")
    public ResponseEntity<ApiResponse<Boolean>> approve(
            @AuthenticationPrincipal Member operator,
            @PathVariable Long refundId,
            @RequestBody RefundDto.ApproveRequest request) {
        refundService.approveRefund(refundId, operator.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    /** 환불 반려 */
    @PutMapping("/{refundId}/reject")
    public ResponseEntity<ApiResponse<Boolean>> reject(
            @AuthenticationPrincipal Member operator,
            @PathVariable Long refundId,
            @RequestBody @Valid RefundDto.RejectRequest request) {
        refundService.rejectRefund(refundId, operator.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(true));
    }
}
