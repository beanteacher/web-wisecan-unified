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

    /** 발급 (02 §5.1) */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyDto.CreateResponse>> create(
            @RequestBody @Valid ApiKeyDto.CreateRequest request) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(apiKeyService.create(memberId, request)));
    }

    /** 내 키 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyDto.Response>>> getMyKeys() {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.getMyKeys(memberId)));
    }

    /** 폐기 (02 §5.4) */
    @PatchMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable Long id) {
        Long memberId = memberService.getCurrentMemberId();
        apiKeyService.revoke(memberId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 재발급 (02 §5.4 rotate).
     * 기존 키 즉시 폐기 + 동일 설정으로 새 키 발급. rawKey 1회 반환.
     */
    @PostMapping("/{id}/rotate")
    public ResponseEntity<ApiResponse<ApiKeyDto.CreateResponse>> rotate(@PathVariable Long id) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(apiKeyService.rotate(memberId, id)));
    }

    /**
     * 스코프·한도 수정 (02 §5.3).
     * 허용 스코프 목록, 일일 한도, 발신번호 화이트리스트를 변경한다.
     */
    @PatchMapping("/{id}/scopes")
    public ResponseEntity<ApiResponse<ApiKeyDto.Response>> updateScopes(
            @PathVariable Long id,
            @RequestBody @Valid ApiKeyDto.UpdateScopesRequest request) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.updateScopes(memberId, id, request)));
    }

    /**
     * 스코프 카탈로그 조회 (02 §5.3).
     * 12종 스코프 전체 목록과 권장 프리셋을 반환한다. 인증 불필요.
     */
    @GetMapping("/scopes/catalog")
    public ResponseEntity<ApiResponse<ApiKeyDto.ScopeCatalogResponse>> getScopeCatalog() {
        return ResponseEntity.ok(ApiResponse.success(apiKeyService.getScopeCatalog()));
    }
}
