package com.wisecan.unified.controller.cs;

import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.domain.cs.InquiryStatus;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.cs.InquiryDto;
import com.wisecan.unified.service.cs.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/cs/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // ── 회원 API ──────────────────────────────────────────────

    /** 1:1 문의 등록 */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InquiryDto.Detail>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid InquiryDto.CreateRequest request) {
        InquiryDto.Detail detail = inquiryService.create(principal.getMemberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(detail));
    }

    /** 내 문의 목록 */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<InquiryDto.Summary>>> myList(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                inquiryService.listByMember(principal.getMemberId(), pageable)));
    }

    /** 내 문의 단건 조회 */
    @GetMapping("/me/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InquiryDto.Detail>> myDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                inquiryService.detailByMember(principal.getMemberId(), id)));
    }

    /** 회원이 문의 종료 처리 */
    @PatchMapping("/me/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> close(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        inquiryService.close(principal.getMemberId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 관리자 API ────────────────────────────────────────────

    /** 전체 문의 목록 */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InquiryDto.Summary>>> adminList(
            @RequestParam(required = false) InquiryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InquiryDto.Summary> page = status != null
                ? inquiryService.listByStatus(status, pageable)
                : inquiryService.listAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /** 관리자 문의 단건 조회 */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryDto.Detail>> adminDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.detail(id)));
    }

    /** 처리 중 상태 전환 */
    @PatchMapping("/admin/{id}/in-progress")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryDto.Detail>> markInProgress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.markInProgress(id)));
    }

    /** 답변 등록 */
    @PostMapping("/admin/{id}/answer")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryDto.Detail>> answer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid InquiryDto.AnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                inquiryService.answer(id, principal.getMemberId(), request)));
    }

    /** SLA 통계 조회 */
    @GetMapping("/admin/sla-stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<InquiryDto.SlaStats>> slaStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.slaStats(from, to)));
    }
}
