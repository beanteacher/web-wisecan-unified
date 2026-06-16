package com.wisecan.unified.controller.cs;

import com.wisecan.unified.domain.cs.InquiryCategory;
import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.cs.FaqDto;
import com.wisecan.unified.service.cs.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cs/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    // ── 회원/공개 API ─────────────────────────────────────────

    /** 노출 FAQ 전체 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FaqDto.Response>>> list(
            @RequestParam(required = false) InquiryCategory category) {
        List<FaqDto.Response> result = category != null
                ? faqService.listByCategory(category)
                : faqService.listVisible();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── 관리자 API ────────────────────────────────────────────

    /** 관리자: 전체 FAQ 목록 (숨김 포함) */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<FaqDto.Response>>> adminList() {
        return ResponseEntity.ok(ApiResponse.success(faqService.listAll()));
    }

    /** 관리자: FAQ 등록 */
    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FaqDto.Response>> create(
            @RequestBody @Valid FaqDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(faqService.create(request)));
    }

    /** 관리자: FAQ 수정 */
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<FaqDto.Response>> update(
            @PathVariable Long id,
            @RequestBody @Valid FaqDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(faqService.update(id, request)));
    }

    /** 관리자: FAQ 삭제 */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
