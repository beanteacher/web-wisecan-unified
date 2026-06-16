package com.wisecan.unified.controller.dispatch;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.dispatch.BalanceBranchDto;
import com.wisecan.unified.exception.InsufficientFundsException;
import com.wisecan.unified.service.billing.BalanceBranchResult;
import com.wisecan.unified.service.billing.InsufficientBalanceRouter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 부족 분기 컨트롤러 — W-405.
 *
 * 02_FEATURE_SPEC §11.1: 발송 전 잔액 부족 평가 및 분기 선택 엔드포인트.
 *
 * POST /dispatch/balance-branch
 *   - 요청: 총 차감 예정액, 수신자 목록, 분기 선택(부분/취소)
 *   - 응답:
 *     200 — 자동결제 또는 후불 처리 완료 (발송 계속)
 *     207 — 부분 발송 (일부 수신자만 적재)
 *     402 — 전체 취소
 */
@RestController
@RequestMapping("/dispatch")
@RequiredArgsConstructor
public class BalanceBranchController {

    private final InsufficientBalanceRouter insufficientBalanceRouter;

    /**
     * API Key 기반 발송 — 잔액 부족 분기 평가.
     *
     * <p>POST /dispatch/balance-branch</p>
     */
    @PostMapping("/balance-branch")
    public ResponseEntity<?> evaluateBranch(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @RequestBody @Valid BalanceBranchDto.EvaluateRequest request
    ) {
        BalanceBranchResult result = insufficientBalanceRouter.route(
                principal.getMemberId(),
                request.companyId(),
                request.totalCost(),
                request.allRecipients(),
                request.unitCost(),
                request.partialChoice()
        );

        return buildResponse(result);
    }

    /**
     * 웹 콘솔(JWT) 기반 발송 — 잔액 부족 분기 평가.
     *
     * <p>POST /console/balance-branch</p>
     */
    @PostMapping("/console/balance-branch")
    public ResponseEntity<?> evaluateBranchWeb(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid BalanceBranchDto.EvaluateRequest request
    ) {
        BalanceBranchResult result = insufficientBalanceRouter.route(
                principal.memberId(),
                request.companyId(),
                request.totalCost(),
                request.allRecipients(),
                request.unitCost(),
                request.partialChoice()
        );

        return buildResponse(result);
    }

    private ResponseEntity<?> buildResponse(BalanceBranchResult result) {
        return switch (result) {
            case BalanceBranchResult.AutoCharged ac ->
                    ResponseEntity.ok(ApiResponse.success(
                            new BalanceBranchDto.AutoChargedResponse(ac.chargedAmount())));

            case BalanceBranchResult.Postpaid pp ->
                    ResponseEntity.ok(ApiResponse.success(
                            new BalanceBranchDto.PostpaidResponse(pp.deferredAmount())));

            case BalanceBranchResult.Partial p ->
                    ResponseEntity.status(HttpStatus.MULTI_STATUS)
                            .body(new BalanceBranchDto.PartialResponse(
                                    p.acceptedCount(),
                                    p.rejectedCount(),
                                    p.rejectedNumbers(),
                                    "INSUFFICIENT_BALANCE",
                                    p.shortfall()));

            case BalanceBranchResult.Cancelled c ->
                    throw new InsufficientFundsException(c.shortfall());
        };
    }
}
