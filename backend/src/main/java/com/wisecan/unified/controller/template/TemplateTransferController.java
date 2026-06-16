package com.wisecan.unified.controller.template;

import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.service.template.TemplateTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SMS17 이관 처리 큐 REST API.
 * 02_FEATURE_SPEC §9.1 이관 처리 참조.
 */
@RestController
@RequestMapping("/api/v1/templates/transfer")
@RequiredArgsConstructor
public class TemplateTransferController {

    private final TemplateTransferService templateTransferService;

    // ── 회원 엔드포인트 ───────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> requestTransfer(@AuthenticationPrincipal UserDetails user,
                                             @RequestBody @Valid TemplateDto.TransferRequest request) {
        TemplateDto.TransferResponse response =
                templateTransferService.requestTransfer(user.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<?> listMyTransfers(@AuthenticationPrincipal UserDetails user) {
        List<TemplateDto.TransferResponse> result =
                templateTransferService.listMyTransfers(user.getUsername());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<?> detailMyTransfer(@AuthenticationPrincipal UserDetails user,
                                              @PathVariable Long transferId) {
        return ResponseEntity.ok(
                templateTransferService.detailMyTransfer(user.getUsername(), transferId));
    }

    @DeleteMapping("/{transferId}")
    public ResponseEntity<?> cancelTransfer(@AuthenticationPrincipal UserDetails user,
                                            @PathVariable Long transferId) {
        templateTransferService.cancelTransfer(user.getUsername(), transferId);
        return ResponseEntity.ok(true);
    }

    // ── 운영자 엔드포인트 ─────────────────────────────────────────

    @GetMapping("/admin/queue")
    public ResponseEntity<?> listPendingQueue() {
        List<TemplateDto.TransferDetail> result = templateTransferService.listPendingQueue();
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/admin/{transferId}/start")
    public ResponseEntity<?> startProcess(@PathVariable Long transferId,
                                          @RequestParam Long operatorId) {
        templateTransferService.startProcess(transferId, operatorId);
        return ResponseEntity.ok(true);
    }

    @PatchMapping("/admin/{transferId}/process")
    public ResponseEntity<?> processTransfer(@PathVariable Long transferId,
                                             @RequestParam Long operatorId,
                                             @RequestBody @Valid TemplateDto.TransferProcessRequest request) {
        TemplateDto.TransferDetail result =
                templateTransferService.processTransfer(transferId, operatorId, request);
        return ResponseEntity.ok(result);
    }
}
