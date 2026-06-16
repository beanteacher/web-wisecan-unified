package com.wisecan.unified.dto.template;

import com.wisecan.unified.domain.template.KakaoTemplateInfo;
import com.wisecan.unified.domain.template.RcsTemplateInfo;
import com.wisecan.unified.domain.template.TemplateTransferQueue;
import com.wisecan.unified.domain.template.TemplateTransferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 카카오·RCS 템플릿/브랜드 DTO.
 * 02_FEATURE_SPEC §9 참조.
 * 컨벤션: dto/{Domain}Dto.java 내 inner record 선언 (.claude/rules/backend-conventions.md).
 */
public class TemplateDto {

    // ── 카카오 템플릿 ────────────────────────────────────────────────

    /**
     * 카카오 알림톡 템플릿 등록 요청.
     */
    public record KakaoRegisterRequest(
            @NotBlank String templateName,
            @NotBlank String templateContent,
            @NotBlank String messageType,
            @NotBlank String categoryCodeM,
            @NotBlank String categoryCodeS,
            String buttons,
            boolean securityFlag
    ) {}

    /**
     * 카카오 알림톡 템플릿 조회 응답.
     * 중계사 정보(kko_profile_no 등)는 포함하지 않는다 (INV-02).
     */
    public record KakaoTemplateResponse(
            String templateCode,
            String templateName,
            String templateContent,
            String inspectionStatus,
            String templateStatus,
            String messageType,
            String categoryCode,
            String buttons,
            boolean securityFlag,
            boolean sendable
    ) {
        public static KakaoTemplateResponse from(KakaoTemplateInfo info) {
            return new KakaoTemplateResponse(
                    info.templateCode(),
                    info.templateName(),
                    info.templateContent(),
                    info.inspectionStatus() != null ? info.inspectionStatus().name() : null,
                    info.templateStatus() != null ? info.templateStatus().name() : null,
                    info.messageType(),
                    info.categoryCode(),
                    info.buttons(),
                    info.securityFlag(),
                    info.isSendable()
            );
        }
    }

    // ── RCS 템플릿 ───────────────────────────────────────────────────

    /**
     * RCS 템플릿 조회 응답.
     * 라우팅 정보(agency_id 등)는 포함하지 않는다 (INV-02).
     */
    public record RcsTemplateResponse(
            String messagebaseId,
            String templateName,
            String brandId,
            String usageStatus,
            String approvalResult,
            String approvalReason,
            String productCode,
            String spec,
            String cardType,
            String inputText,
            boolean sendable
    ) {
        public static RcsTemplateResponse from(RcsTemplateInfo info) {
            return new RcsTemplateResponse(
                    info.messagebaseId(),
                    info.templateName(),
                    info.brandId(),
                    info.usageStatus() != null ? info.usageStatus().name() : null,
                    info.approvalResult() != null ? info.approvalResult().getExternalValue() : null,
                    info.approvalReason(),
                    info.productCode(),
                    info.spec(),
                    info.cardType(),
                    info.inputText(),
                    info.isSendable()
            );
        }
    }

    // ── 이관 처리 큐 ─────────────────────────────────────────────────

    /**
     * SMS17 이관 신청 요청.
     */
    public record TransferRequest(
            @NotBlank String sourceTemplateCode,
            Integer kkoProfileNo,
            String reason
    ) {}

    /**
     * 이관 신청 응답.
     */
    public record TransferResponse(
            Long id,
            Long memberId,
            String sourceTemplateCode,
            TemplateTransferStatus status,
            LocalDateTime requestedAt
    ) {
        public static TransferResponse from(TemplateTransferQueue queue) {
            return new TransferResponse(
                    queue.getId(),
                    queue.getMemberId(),
                    queue.getSourceTemplateCode(),
                    queue.getStatus(),
                    queue.getRequestedAt()
            );
        }
    }

    /**
     * 이관 신청 상세 응답 (운영자용).
     */
    public record TransferDetail(
            Long id,
            Long memberId,
            String sourceTemplateCode,
            Integer kkoProfileNo,
            String reason,
            TemplateTransferStatus status,
            String rejectReason,
            Long operatorId,
            LocalDateTime requestedAt,
            LocalDateTime resolvedAt
    ) {
        public static TransferDetail from(TemplateTransferQueue queue) {
            return new TransferDetail(
                    queue.getId(),
                    queue.getMemberId(),
                    queue.getSourceTemplateCode(),
                    queue.getKkoProfileNo(),
                    queue.getReason(),
                    queue.getStatus(),
                    queue.getRejectReason(),
                    queue.getOperatorId(),
                    queue.getRequestedAt(),
                    queue.getResolvedAt()
            );
        }
    }

    /**
     * 운영자 이관 처리 요청 (완료/거부).
     */
    public record TransferProcessRequest(
            @NotNull boolean approve,
            String rejectReason
    ) {}
}
