package com.wisecan.unified.controller.trial;

import com.wisecan.unified.dto.trial.TrialDto;
import com.wisecan.unified.service.trial.TrialSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 체험 모드 API 컨트롤러 (W-406).
 *
 * <p>비회원 체험 세션 발급·발송·결제 차단·종료 엔드포인트.</p>
 *
 * <p>모든 엔드포인트는 인증 불필요 (익명 + 체험 세션 토큰만 사용).</p>
 */
@RestController
@RequestMapping("/trial")
@RequiredArgsConstructor
public class TrialController {

    private final TrialSessionService trialSessionService;

    /**
     * 체험 세션을 발급한다.
     *
     * <p>POST /trial/sessions</p>
     *
     * @param httpRequest 클라이언트 IP 추출용
     * @return 세션 토큰 + 더미 컨텍스트
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> issueSession(HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        TrialDto.SessionResponse response = trialSessionService.issueSession(clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * 체험 발송을 처리한다 (외부 송출 없음).
     *
     * <p>POST /trial/sessions/{sessionToken}/send</p>
     *
     * @param sessionToken 체험 세션 토큰
     * @param request      발송 요청 DTO
     * @return 가상 발송 결과
     */
    @PostMapping("/sessions/{sessionToken}/send")
    public ResponseEntity<?> trialSend(
            @PathVariable String sessionToken,
            @RequestBody @Valid TrialDto.SendRequest request
    ) {
        TrialDto.SendResponse response = trialSessionService.trialSend(sessionToken, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 체험 결제/충전 시도를 차단한다.
     *
     * <p>POST /trial/sessions/{sessionToken}/billing</p>
     *
     * @param sessionToken 체험 세션 토큰
     * @return 결제 차단 응답
     */
    @PostMapping("/sessions/{sessionToken}/billing")
    public ResponseEntity<?> blockBilling(@PathVariable String sessionToken) {
        TrialDto.BillingBlockedResponse response = trialSessionService.blockBilling(sessionToken);
        return ResponseEntity.ok(response);
    }

    /**
     * 체험 세션을 종료한다.
     *
     * <p>DELETE /trial/sessions/{sessionToken}</p>
     *
     * @param sessionToken 체험 세션 토큰
     */
    @DeleteMapping("/sessions/{sessionToken}")
    public ResponseEntity<?> closeSession(@PathVariable String sessionToken) {
        trialSessionService.closeSession(sessionToken);
        return ResponseEntity.ok(true);
    }

    /** X-Forwarded-For 헤더 우선, 없으면 RemoteAddr 사용 */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
