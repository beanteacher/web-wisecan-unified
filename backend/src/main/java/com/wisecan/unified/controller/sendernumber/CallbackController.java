package com.wisecan.unified.controller.sendernumber;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.sendernumber.CallbackDto;
import com.wisecan.unified.service.sendernumber.CallbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 발신번호 API — 4 케이스 등록 / 삭제 / 목록·단건 조회.
 *
 * URL: /api/v1/callbacks
 * 권한: JWT 인증 필요 (callback:manage 스코프는 서비스 레이어에서 검증)
 */
@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
public class CallbackController {

    private final CallbackService callbackService;

    /**
     * 발신번호 등록 (4 케이스 공통 진입점).
     * - SELF_MOBILE / SELF_LANDLINE : 즉시 REGISTERED → 201
     * - EMPLOYEE / CORP_REP         : SUBMITTED (심사 큐) → 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CallbackDto.RegisterResponse>> register(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody @Valid CallbackDto.RegisterRequest request
    ) {
        CallbackDto.RegisterResponse response = callbackService.register(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** 발신번호 목록 조회 (DELETED 제외) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CallbackDto.Summary>>> list(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<CallbackDto.Summary> result = callbackService.list(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 발신번호 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CallbackDto.Response>> detail(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long id
    ) {
        CallbackDto.Response response = callbackService.detail(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 발신번호 삭제 (§4.4) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long id
    ) {
        callbackService.delete(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
