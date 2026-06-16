package com.wisecan.unified.controller.dispatch;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.service.dispatch.SendHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 발송 이력 조회 컨트롤러 (W-304).
 *
 * <p>02_FEATURE_SPEC.md §8.1 — 기간·채널·발신번호·수신번호 필터 + 페이지네이션 + 키별 조회 범위 정책.</p>
 *
 * <p>인증: ApiKeyAuthFilter 가 {@code Authorization: Bearer <key>} 헤더를 처리하여
 * {@link ApiKeyPrincipal}을 SecurityContext 에 주입한다.</p>
 *
 * <p>범위 정책:
 * <ul>
 *   <li>기본(scope:key) — principal.apiKeyId() 기준 조회</li>
 *   <li>scope:member — scopeMember=true 파라미터 전달 시 principal.memberId() 기준 조회
 *       (서비스 레이어에서 HISTORY_READ 스코프 보유 여부 검증)</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/histories")
@RequiredArgsConstructor
public class SendHistoryController {

    private final SendHistoryService sendHistoryService;

    /**
     * 발송 이력 목록 조회.
     *
     * <pre>
     * GET /histories
     *   ?fromDate=2026-01-01T00:00:00
     *   &amp;toDate=2026-12-31T23:59:59
     *   &amp;channel=SMS
     *   &amp;callbackNumber=01012345678
     *   &amp;recipientNumber=01098765432
     *   &amp;status=QUEUED
     *   &amp;scopeMember=false
     *   &amp;page=0&amp;size=20
     * </pre>
     */
    @GetMapping
    public ResponseEntity<?> list(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate,
            @RequestParam(required = false) SendChannel channel,
            @RequestParam(required = false) String callbackNumber,
            @RequestParam(required = false) String recipientNumber,
            @RequestParam(required = false) SendRequestStatus status,
            @RequestParam(defaultValue = "false") boolean scopeMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                fromDate, toDate, channel, callbackNumber, recipientNumber, status);

        // scope:key(기본) = apiKeyId 전달, scope:member = apiKeyId null 로 memberId 기준 조회
        Long apiKeyId = scopeMember ? null : principal.apiKeyId();

        SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                sendHistoryService.list(
                        principal.memberId(),
                        apiKeyId,
                        params,
                        page,
                        Math.min(size, 100)
                );
        return ResponseEntity.ok(result);
    }

    /**
     * 발송 이력 단건 상세 조회.
     *
     * <pre>GET /histories/{sendId}</pre>
     *
     * <p>접근 범위 정책: scope:key(기본) 또는 scope:member(scopeMember=true).
     * 접근 불가 이력은 404로 응답하여 존재 자체를 숨긴다.</p>
     */
    @GetMapping("/{sendId}")
    public ResponseEntity<?> detail(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @PathVariable String sendId,
            @RequestParam(defaultValue = "false") boolean scopeMember
    ) {
        Long apiKeyId = scopeMember ? null : principal.apiKeyId();

        SendHistoryDto.DetailItem result = sendHistoryService.detail(
                principal.memberId(),
                apiKeyId,
                sendId
        );
        return ResponseEntity.ok(result);
    }
}
