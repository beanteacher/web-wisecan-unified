package com.wisecan.unified.controller.dispatch;

import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.dispatch.WebSendDto;
import com.wisecan.unified.service.dispatch.WebSendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 웹 콘솔 발송 컨트롤러 (W-206).
 *
 * <p>JWT 인증 기반 회원 세션 엔드포인트. {@link UserPrincipal}에서 memberId를 추출한다.</p>
 *
 * <p>경로: /console/send — 03_IA.md §도메인 라우팅에 따라 /api/v1 prefix 없음.</p>
 *
 * <p>지원 엔드포인트 (02_FEATURE_SPEC.md §6):</p>
 * <ul>
 *   <li>POST /console/send           — 단건 발송 (§6.1, 5채널, 수신자 최대 1,000개)</li>
 *   <li>POST /console/send/bulk      — 일괄 발송 (§6.2, CSV 최대 100,000행)</li>
 *   <li>POST /console/send/scheduled — 예약 발송 (§6.3)</li>
 *   <li>GET  /console/send/scheduled — 예약 목록 조회</li>
 *   <li>DELETE /console/send/scheduled/{sendId} — 예약 취소</li>
 * </ul>
 */
@RestController
@RequestMapping("/console/send")
@RequiredArgsConstructor
public class WebSendController {

    private final WebSendService webSendService;

    /**
     * 단건 발송 — 5채널 지원, 수신자 최대 1,000개.
     *
     * <p>POST /console/send</p>
     */
    @PostMapping
    public ResponseEntity<?> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid WebSendDto.SingleRequest request
    ) {
        WebSendDto.AcceptResponse response = webSendService.send(
                principal.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 일괄 발송 — CSV 파싱 결과 최대 100,000행.
     *
     * <p>POST /console/send/bulk</p>
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> sendBulk(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid WebSendDto.BulkRequest request
    ) {
        WebSendDto.AcceptResponse response = webSendService.sendBulk(
                principal.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 발송 등록 — 발송 시각 이전이면 취소 가능.
     *
     * <p>POST /console/send/scheduled</p>
     */
    @PostMapping("/scheduled")
    public ResponseEntity<?> sendScheduled(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid WebSendDto.ScheduledRequest request
    ) {
        WebSendDto.AcceptResponse response = webSendService.sendScheduled(
                principal.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 예약 발송 목록 조회 — PENDING 상태의 미래 예약만 반환.
     *
     * <p>GET /console/send/scheduled</p>
     */
    @GetMapping("/scheduled")
    public ResponseEntity<?> listScheduled(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<WebSendDto.ScheduledSummary> page = webSendService.listScheduled(
                principal.memberId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * 예약 발송 취소 — 발송 시각 이전 PENDING 상태만 취소 가능.
     *
     * <p>DELETE /console/send/scheduled/{sendId}</p>
     */
    @DeleteMapping("/scheduled/{sendId}")
    public ResponseEntity<?> cancelScheduled(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String sendId,
            @RequestBody(required = false) WebSendDto.CancelRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        webSendService.cancelScheduled(principal.memberId(), sendId, reason);
        return ResponseEntity.ok(ApiResponse.success(true));
    }
}
