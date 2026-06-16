package com.wisecan.unified.dto;

import com.wisecan.unified.domain.BusinessApplication;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.ApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 운영자 콘솔 검토 큐 DTO — W-106
 *
 * §12.1 사업자 전환 심사 / §12.2 발신번호 심사 / §12.6 API 키 검토 큐
 */
public class AdminReviewDto {

    // ── §12.1 사업자 전환 심사 ──────────────────────────────────────

    /** 심사 큐 목록 항목 */
    public record BusinessApplicationSummary(
        Long applicationId,
        Long memberId,
        String memberEmail,
        String bizNumber,
        String companyName,
        String ceoName,
        String ceoPhone,
        String status,
        LocalDateTime submittedAt
    ) {
        public static BusinessApplicationSummary from(BusinessApplication app, String memberEmail) {
            return new BusinessApplicationSummary(
                app.getId(),
                app.getMemberId(),
                memberEmail,
                app.getBizNumber(),
                app.getCompanyName(),
                app.getCeoName(),
                app.getCeoPhone(),
                app.getStatus(),
                app.getSubmittedAt()
            );
        }
    }

    /** 심사 상세 */
    public record BusinessApplicationDetail(
        Long applicationId,
        Long memberId,
        String memberEmail,
        String bizNumber,
        String corpNumber,
        String companyName,
        String ceoName,
        String ceoPhone,
        String status,
        String rejectReason,
        Long reviewedBy,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
    ) {
        public static BusinessApplicationDetail from(BusinessApplication app, String memberEmail) {
            return new BusinessApplicationDetail(
                app.getId(),
                app.getMemberId(),
                memberEmail,
                app.getBizNumber(),
                app.getCorpNumber(),
                app.getCompanyName(),
                app.getCeoName(),
                app.getCeoPhone(),
                app.getStatus(),
                app.getRejectReason(),
                app.getReviewedBy(),
                app.getSubmittedAt(),
                app.getReviewedAt()
            );
        }
    }

    /** 심사 시작 (SUBMITTED → UNDER_REVIEW) */
    public record StartReviewRequest() {}

    /** 승인 요청 */
    public record ApproveBusinessRequest() {}

    /** 반려 요청 */
    public record RejectBusinessRequest(
        @NotBlank(message = "반려 사유는 필수입니다") String reason
    ) {}

    // ── §12.2 발신번호 심사 ─────────────────────────────────────────

    /** 발신번호 심사 큐 항목 */
    public record CallbackReviewSummary(
        Long callbackId,
        Long memberId,
        String memberEmail,
        String phoneNumber,
        String registerType,
        String description,
        String status,
        LocalDateTime createdAt
    ) {
        public static CallbackReviewSummary from(Callback cb, String memberEmail) {
            return new CallbackReviewSummary(
                cb.getId(),
                cb.getMemberId(),
                memberEmail,
                cb.getPhoneNumber(),
                cb.getRegisterType() != null ? cb.getRegisterType().name() : null,
                cb.getDescription(),
                cb.getStatus().name(),
                cb.getCreatedAt()
            );
        }
    }

    /** 발신번호 승인 */
    public record ApproveCallbackRequest() {}

    /** 발신번호 반려 */
    public record RejectCallbackRequest(
        @NotBlank(message = "반려 사유는 필수입니다") String reason
    ) {}

    // ── §12.6 API 키 검토 큐 ───────────────────────────────────────

    /** API 키 검토 큐 항목 */
    public record ApiKeyReviewSummary(
        Long apiKeyId,
        Long memberId,
        String memberEmail,
        String keyName,
        String keyPrefix,
        String keyType,
        String status,
        LocalDateTime createdAt
    ) {
        public static ApiKeyReviewSummary from(ApiKey key, String memberEmail) {
            return new ApiKeyReviewSummary(
                key.getId(),
                key.getMember().getId(),
                memberEmail,
                key.getKeyName(),
                key.getKeyPrefix(),
                key.getKeyType().name(),
                key.getStatus().name(),
                key.getCreatedAt()
            );
        }
    }

    /** API 키 승인 (PENDING_REVIEW → ACTIVE) */
    public record ApproveApiKeyRequest() {}

    /** API 키 반려 */
    public record RejectApiKeyRequest(
        @NotBlank(message = "반려 사유는 필수입니다") String reason
    ) {}

    /** 운영자 ID를 담는 공통 헤더 — 컨트롤러에서 JWT principal로 추출 */
    public record AdminContext(@NotNull Long adminId) {}
}
