package com.wisecan.unified.controller.admin;

import com.wisecan.unified.domain.admin.RoutingChannel;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.admin.RoutingMappingDto;
import com.wisecan.unified.service.admin.RoutingMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카카오/RCS 라우팅 매핑 API — W-501, §12.4.
 *
 * 회원에게는 어떤 형태로도 노출하지 않음 (응답·UI·로그·이력).
 * 운영자 전용 엔드포인트.
 *
 * POST   /api/v1/admin/routing/mappings              매핑 등록/수정 (Upsert)
 * GET    /api/v1/admin/routing/mappings/member/{id}  회원별 매핑 목록
 * GET    /api/v1/admin/routing/mappings/channel/{ch} 채널별 매핑 목록
 * DELETE /api/v1/admin/routing/mappings/{mappingId}  매핑 삭제
 */
@RestController
@RequestMapping("/api/v1/admin/routing/mappings")
@RequiredArgsConstructor
public class AdminRoutingController {

    private final RoutingMappingService routingMappingService;
    private final AdminAuthHelper adminAuthHelper;

    /** 매핑 등록/수정 */
    @PostMapping
    public ResponseEntity<ApiResponse<RoutingMappingDto.Response>> upsert(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid RoutingMappingDto.UpsertRequest request) {
        Long operatorId = resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                routingMappingService.upsert(operatorId, request)));
    }

    /** 회원별 매핑 목록 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<RoutingMappingDto.Response>>> listByMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long memberId) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                routingMappingService.listByMember(memberId)));
    }

    /** 채널별 매핑 목록 */
    @GetMapping("/channel/{channel}")
    public ResponseEntity<ApiResponse<List<RoutingMappingDto.Response>>> listByChannel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable RoutingChannel channel) {
        resolveAdminId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                routingMappingService.listByChannel(channel)));
    }

    /** 매핑 삭제 */
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<ApiResponse<Boolean>> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long mappingId) {
        Long operatorId = resolveAdminId(userDetails);
        routingMappingService.delete(mappingId, operatorId);
        return ResponseEntity.ok(ApiResponse.success(true));
    }

    // ── 내부 헬퍼 ────────────────────────────────────────────────────────

    private Long resolveAdminId(UserDetails userDetails) {
        return adminAuthHelper.resolveAdminId(userDetails);
    }
}
