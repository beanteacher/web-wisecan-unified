package com.wisecan.unified.dto.admin;

import com.wisecan.unified.domain.admin.RoutingChannel;
import com.wisecan.unified.domain.admin.RoutingCarrier;
import com.wisecan.unified.domain.admin.RoutingMapping;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 카카오/RCS 라우팅 매핑 DTO — §12.4.
 *
 * 01. UpsertRequest  — POST /admin/routing/mappings
 * 02. Response       — 매핑 결과 응답
 */
public class RoutingMappingDto {

    /** 라우팅 매핑 등록/수정 요청 */
    public record UpsertRequest(
            @NotNull(message = "회원 ID는 필수입니다")
            Long memberId,

            @NotNull(message = "채널은 필수입니다")
            RoutingChannel channel,

            @NotNull(message = "중계사는 필수입니다")
            RoutingCarrier carrier,

            String memo
    ) {}

    /** 라우팅 매핑 응답 */
    public record Response(
            Long id,
            Long memberId,
            RoutingChannel channel,
            RoutingCarrier carrier,
            String memo,
            Long operatorId,
            LocalDateTime updatedAt
    ) {
        public static Response from(RoutingMapping mapping) {
            return new Response(
                    mapping.getId(),
                    mapping.getMemberId(),
                    mapping.getChannel(),
                    mapping.getCarrier(),
                    mapping.getMemo(),
                    mapping.getOperatorId(),
                    mapping.getUpdatedAt()
            );
        }
    }
}
