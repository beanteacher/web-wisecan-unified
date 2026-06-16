package com.wisecan.unified.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BusinessApplicationDto {

    // ── 회원 요청 ──────────────────────────────────────────────────

    public record SubmitRequest(
        @NotBlank(message = "사업자번호는 필수입니다")
        @Pattern(regexp = "\\d{3}-\\d{2}-\\d{5}", message = "올바른 사업자번호 형식이 아닙니다 (예: 123-45-67890)")
        String bizNumber,

        String corpNumber,

        @NotBlank(message = "회사명은 필수입니다") String companyName,
        @NotBlank(message = "대표자명은 필수입니다") String ceoName,
        String ceoPhone
    ) {}

    public record StatusResponse(
        Long applicationId,
        String status,
        String companyName,
        String bizNumber,
        String rejectReason
    ) {}

    // ── 운영자 요청 (§12.1 — W-106 검토 큐) ──────────────────────

    /** 운영자 반려 요청 */
    public record RejectRequest(
        @NotBlank(message = "반려 사유는 필수입니다")
        @Size(max = 500)
        String reason
    ) {}

    /** 운영자 보완 서류 요청 */
    public record RequestDocumentRequest(
        @NotBlank(message = "요청 내용은 필수입니다")
        @Size(max = 500)
        String message
    ) {}
}
