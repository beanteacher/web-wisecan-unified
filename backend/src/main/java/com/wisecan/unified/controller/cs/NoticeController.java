package com.wisecan.unified.controller.cs;

import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.cs.NoticeDto;
import com.wisecan.unified.service.cs.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cs/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // ── 회원/공개 API ─────────────────────────────────────────

    /** 노출 공지 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeDto.Summary>>> list() {
        return ResponseEntity.ok(ApiResponse.success(noticeService.listVisible()));
    }

    /** 공지 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDto.Detail>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.detail(id)));
    }

    // ── 관리자 API ────────────────────────────────────────────

    /** 관리자: 전체 공지 목록 */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<NoticeDto.Summary>>> adminList(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.listAll(pageable)));
    }

    /** 관리자: 공지 등록 */
    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NoticeDto.Detail>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid NoticeDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(noticeService.create(principal.getMemberId(), request)));
    }

    /** 관리자: 공지 수정 */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<NoticeDto.Detail>> update(
            @PathVariable Long id,
            @RequestBody @Valid NoticeDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.update(id, request)));
    }

    /** 관리자: 공지 삭제 */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
