package com.wisecan.unified.controller.dispatch;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.dispatch.SendRequestDto;
import com.wisecan.unified.service.dispatch.SendRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 발송 요청 적재 컨트롤러.
 *
 * <p>API Key 인증 기반 엔드포인트. {@link ApiKeyPrincipal}에서 memberId·apiKeyId를 추출한다.</p>
 *
 * <p>단가(unitCost)는 임시로 요청 헤더 {@code X-Unit-Cost}로 수신한다.
 * W-204(외부 시스템 인터페이스)에서 채널별 단가 테이블 조회로 교체 예정.</p>
 *
 * <p>W-205: 헤더 {@code X-Network-Type}으로 발송 망(TEST/PRODUCTION)을 명시할 수 있다.
 * 미지정 시 API Key 유형에서 자동 결정된다. 키 유형과 망이 불일치하면 검증 게이트에서 거부된다.</p>
 *
 * <p>경로: /dispatch (03_IA.md §도메인 라우팅, /api/v1 prefix 없음)</p>
 */
@RestController
@RequestMapping("/dispatch")
@RequiredArgsConstructor
public class SendRequestController {

    private final SendRequestService sendRequestService;

    /**
     * 단건 발송 요청 적재.
     *
     * <p>POST /dispatch/send</p>
     */
    @PostMapping("/send")
    public ResponseEntity<?> send(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @RequestHeader(value = "X-Unit-Cost", defaultValue = "20") long unitCost,
            @RequestHeader(value = "X-Network-Type", required = false) NetworkType networkType,
            @RequestBody @Valid SendRequestDto.SingleRequest request
    ) {
        SendRequestDto.AcceptResponse response = sendRequestService.sendSingle(
                principal.memberId(),
                principal.apiKeyId(),
                unitCost,
                networkType,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 다건(일괄) 발송 요청 적재.
     *
     * <p>POST /dispatch/send/bulk</p>
     */
    @PostMapping("/send/bulk")
    public ResponseEntity<?> sendBulk(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @RequestHeader(value = "X-Unit-Cost", defaultValue = "20") long unitCost,
            @RequestHeader(value = "X-Network-Type", required = false) NetworkType networkType,
            @RequestBody @Valid SendRequestDto.BulkRequest request
    ) {
        SendRequestDto.AcceptResponse response = sendRequestService.sendBulk(
                principal.memberId(),
                principal.apiKeyId(),
                unitCost,
                networkType,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 발송 요청 상세 조회 — send_id(ULID)로 조회.
     *
     * <p>GET /dispatch/send/{sendId}</p>
     */
    @GetMapping("/send/{sendId}")
    public ResponseEntity<?> getDetail(
            @AuthenticationPrincipal ApiKeyPrincipal principal,
            @PathVariable String sendId
    ) {
        SendRequestDto.DetailResponse response = sendRequestService.getDetail(sendId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
