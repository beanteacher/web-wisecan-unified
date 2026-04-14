package com.wisecan.unified.controller;

import com.wisecan.unified.dto.ApiKeyDto;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.service.ApiKeyService;
import com.wisecan.unified.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyDto.CreateResponse>> create(@RequestBody @Valid ApiKeyDto.CreateRequest request) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(apiKeyService.create(memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyDto.Response>>> getMyKeys() {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.getMyKeys(memberId)));
    }

    @PatchMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable Long id) {
        Long memberId = memberService.getCurrentMemberId();
        apiKeyService.revoke(memberId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
