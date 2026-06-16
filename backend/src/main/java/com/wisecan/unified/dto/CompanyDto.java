package com.wisecan.unified.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class CompanyDto {

    // ── 하위 계정 생성 ──────────────────────────────────────────────────────────

    public record CreateSubAccountRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다") String password,
        @NotBlank(message = "이름은 필수입니다") String name,
        String phone
    ) {}

    public record SubAccountResponse(
        Long id,
        Long companyId,
        String loginId,
        String name,
        String phone,
        String status,
        LocalDateTime createdAt
    ) {}

    // ── 초대 링크 발행 ──────────────────────────────────────────────────────────

    public record CreateInvitationRequest(
        @NotBlank(message = "초대 대상 이메일은 필수입니다") String inviteeEmail,
        String inviteePhone
    ) {}

    public record InvitationResponse(
        Long id,
        String inviteeEmail,
        String status,
        String token,
        LocalDateTime expiresAt
    ) {}

    // ── 초대 수락 ───────────────────────────────────────────────────────────────

    public record AcceptInvitationRequest(
        @NotBlank(message = "토큰은 필수입니다") String token,
        @NotBlank(message = "비밀번호는 필수입니다") String password,
        @NotBlank(message = "이름은 필수입니다") String name,
        String phone
    ) {}

    // ── 마스터 권한 이관 ────────────────────────────────────────────────────────

    public record TransferMasterRequest(
        @NotNull(message = "새 마스터 회원 ID는 필수입니다") Long toMemberId
    ) {}

    // ── 권한 이력 ───────────────────────────────────────────────────────────────

    public record RoleLogResponse(
        Long id,
        String action,
        String reason,
        Long fromMemberId,
        Long toMemberId,
        Long actorMemberId,
        LocalDateTime actedAt
    ) {}

    // ── 회사 정보 ───────────────────────────────────────────────────────────────

    public record CompanyInfoResponse(
        Long id,
        String name,
        String bizNumber,
        String billingMode,
        String status,
        LocalDateTime approvedAt
    ) {}

    // ── 하위 계정 목록 ──────────────────────────────────────────────────────────

    public record SubAccountListResponse(
        List<SubAccountResponse> members,
        int total
    ) {}
}
