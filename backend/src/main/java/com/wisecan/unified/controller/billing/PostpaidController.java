package com.wisecan.unified.controller.billing;

import com.wisecan.unified.dto.billing.PostpaidDto;
import com.wisecan.unified.service.billing.PostpaidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 후불 모델 컨트롤러 — W-403.
 * 02_FEATURE_SPEC §10.3 / backend-conventions.md 준수.
 *
 * 회원(COMPANY_MASTER) 엔드포인트:
 *   POST   /billing/postpaid/apply              후불 신청
 *   GET    /billing/postpaid/config             후불 설정 조회
 *   GET    /billing/postpaid/invoices           청구서 목록 조회
 *   POST   /billing/postpaid/invoices/{id}/pay  청구서 결제
 *
 * 운영자 엔드포인트:
 *   POST   /billing/postpaid/admin/{id}/review  심사 시작
 *   POST   /billing/postpaid/admin/{id}/approve 승인
 *   POST   /billing/postpaid/admin/{id}/suspend 정지
 *   POST   /billing/postpaid/admin/invoices     청구서 발행
 *
 * 배치 엔드포인트:
 *   POST   /billing/postpaid/batch/overdue      연체 처리
 */
@RestController
@RequestMapping("/billing/postpaid")
@RequiredArgsConstructor
public class PostpaidController {

    private final PostpaidService postpaidService;

    // ─── 회원 ───────────────────────────────────────────

    @PostMapping("/apply")
    public ResponseEntity<?> apply(
            @RequestParam Long companyId,
            @RequestBody @Valid PostpaidDto.ApplyRequest request) {
        return ResponseEntity.ok(postpaidService.apply(companyId, request));
    }

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@RequestParam Long companyId) {
        return ResponseEntity.ok(postpaidService.getConfig(companyId));
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices(@RequestParam Long companyId) {
        return ResponseEntity.ok(postpaidService.listInvoices(companyId));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public ResponseEntity<?> payInvoice(
            @PathVariable Long invoiceId,
            @RequestBody @Valid PostpaidDto.PayInvoiceRequest request) {
        return ResponseEntity.ok(postpaidService.payInvoice(invoiceId, request));
    }

    // ─── 운영자 ─────────────────────────────────────────

    @PostMapping("/admin/{configId}/review")
    public ResponseEntity<?> startReview(@PathVariable Long configId) {
        return ResponseEntity.ok(postpaidService.startReview(configId));
    }

    @PostMapping("/admin/{configId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long configId,
            @RequestBody @Valid PostpaidDto.ApproveRequest request) {
        return ResponseEntity.ok(postpaidService.approve(configId, request));
    }

    @PostMapping("/admin/{configId}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long configId) {
        return ResponseEntity.ok(postpaidService.suspend(configId));
    }

    @PostMapping("/admin/invoices")
    public ResponseEntity<?> issueInvoice(
            @RequestParam Long companyId,
            @RequestBody @Valid PostpaidDto.IssueInvoiceRequest request) {
        return ResponseEntity.ok(postpaidService.issueInvoice(companyId, request));
    }

    // ─── 배치 ───────────────────────────────────────────

    @PostMapping("/batch/overdue")
    public ResponseEntity<?> processOverdue() {
        int count = postpaidService.processOverdue();
        return ResponseEntity.ok(count + "건 연체 처리 완료");
    }
}
