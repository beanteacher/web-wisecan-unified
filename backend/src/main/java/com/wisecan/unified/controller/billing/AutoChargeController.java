package com.wisecan.unified.controller.billing;

import com.wisecan.unified.dto.billing.AutoChargeDto;
import com.wisecan.unified.service.billing.AutoChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 자동충전 설정 API — W-402.
 *
 * POST   /billing/auto-charge          자동충전 활성화·갱신
 * GET    /billing/auto-charge          자동충전 설정 조회
 * DELETE /billing/auto-charge          자동충전 해지
 *
 * MVP: memberId 는 요청 헤더 X-Member-Id 로 전달 (실 서비스에서 JWT 인증으로 대체).
 */
@RestController
@RequestMapping("/billing/auto-charge")
@RequiredArgsConstructor
public class AutoChargeController {

    private final AutoChargeService autoChargeService;

    @PostMapping
    public ResponseEntity<?> activate(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody @Valid AutoChargeDto.ActivateRequest request) {
        AutoChargeDto.Response response = autoChargeService.activate(memberId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> getConfig(
            @RequestHeader("X-Member-Id") Long memberId) {
        return ResponseEntity.ok(autoChargeService.getConfig(memberId));
    }

    @DeleteMapping
    public ResponseEntity<?> deactivate(
            @RequestHeader("X-Member-Id") Long memberId) {
        autoChargeService.deactivate(memberId);
        return ResponseEntity.ok(true);
    }
}
