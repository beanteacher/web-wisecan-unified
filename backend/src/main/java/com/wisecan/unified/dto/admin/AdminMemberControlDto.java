package com.wisecan.unified.dto.admin;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.admin.ControlAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 운영자 회원 통제 DTO — §12.3.
 *
 * 01. SuspendRequest    — POST /admin/members/{memberId}/suspend
 * 02. TerminateRequest  — POST /admin/members/{memberId}/terminate
 * 03. UnsuspendRequest  — POST /admin/members/{memberId}/unsuspend
 * 04. MemberStatusResponse — 상태 변경 결과 응답
 */
public class AdminMemberControlDto {

    /** 강제 정지 요청 */
    public record SuspendRequest(
            @NotBlank(message = "정지 사유는 필수입니다")
            String reason
    ) {}

    /** 강제 해지 요청 */
    public record TerminateRequest(
            @NotBlank(message = "해지 사유는 필수입니다")
            String reason
    ) {}

    /** 정지 해제 요청 */
    public record UnsuspendRequest(
            @NotBlank(message = "해제 사유는 필수입니다")
            String reason
    ) {}

    /** 회원 상태 변경 결과 응답 */
    public record MemberStatusResponse(
            Long memberId,
            String email,
            String name,
            MemberRole role,
            MemberStatus status,
            LocalDateTime updatedAt
    ) {
        public static MemberStatusResponse from(Member member) {
            return new MemberStatusResponse(
                    member.getId(),
                    member.getEmail(),
                    member.getName(),
                    member.getRole(),
                    member.getStatus(),
                    member.getUpdatedAt()
            );
        }
    }

    /**
     * 회원 통제 감사 로그 항목.
     * M-1 리뷰 반영: action 타입을 String → ControlAction 으로 변경.
     */
    public record ControlAuditEntry(
            Long memberId,
            String email,
            ControlAction action,
            String reason,
            Long operatorId,
            LocalDateTime occurredAt
    ) {}
}
