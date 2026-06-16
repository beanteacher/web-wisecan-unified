package com.wisecan.unified.controller.billing;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.billing.ChargeDto;
import com.wisecan.unified.service.billing.BalanceQueryService;
import com.wisecan.unified.service.billing.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 충전 컨트롤러 — W-401.
 *
 * POST /billing/charge      수동 충전
 * GET  /billing/balance     잔액 조회
 */
@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final BalanceQueryService balanceQueryService;

    /**
     * 수동 충전 — PREPAID 회원 전용.
     * POSTPAID 회원은 BillingException → 400 Bad Request.
     */
    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<ChargeDto.Response>> charge(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid ChargeDto.CreateRequest request) {
        ChargeDto.Response response = chargeService.charge(
                member.getId(),
                member.getCompany() != null ? member.getCompany().getBillingMode() : "PREPAID",
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 잔액 조회 — Redis 캐시 우선, MISS 시 DB.
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<ChargeDto.BalanceResponse>> balance(
            @AuthenticationPrincipal Member member) {
        ChargeDto.BalanceResponse response = balanceQueryService.getBalance(member.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
