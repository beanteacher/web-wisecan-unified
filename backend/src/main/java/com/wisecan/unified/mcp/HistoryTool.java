package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.service.dispatch.SendHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * wsc.history.list / wsc.history.detail — 발송 이력 조회 (RQ-MCP-006, 007).
 *
 * <p>비노출 정책 (§7.3 / INV-02): routingMeta(내부 라우팅 메타)는 응답에 포함하지 않는다.
 * SendHistoryDto.ListItem / DetailItem 은 이미 routingMeta 를 제외한다.</p>
 *
 * <p>동등성 (§7.4): SendHistoryService 를 직접 호출하여 CLI wsc history list/detail 과 동일 결과.</p>
 */
@Component
@RequiredArgsConstructor
public class HistoryTool {

    private final SendHistoryService sendHistoryService;

    @Tool(description = """
            발송 이력 목록을 조회한다. 기간·채널·발신번호·수신번호·상태 필터 지원.
            필수 스코프: history:read
            응답: { content: [{ sendId, channel, callbackNumber, recipientCount, status, totalCost, requestedAt }], page, size, totalElements, totalPages }
            (주의: routingMeta 등 내부 라우팅 정보는 포함되지 않음)
            """)
    public SendHistoryDto.PageResponse<SendHistoryDto.ListItem> historyList(
            @ToolParam(description = "조회 시작일시 ISO 8601 (null = 제한 없음)") String fromDate,
            @ToolParam(description = "조회 종료일시 ISO 8601 (null = 제한 없음)") String toDate,
            @ToolParam(description = "채널 필터 (SMS | LMS | MMS | KAKAO | RCS | null = 전체)") String channel,
            @ToolParam(description = "발신번호 필터 (null = 전체)") String callbackNumber,
            @ToolParam(description = "수신번호 포함 검색 (null = 전체)") String recipientNumber,
            @ToolParam(description = "상태 필터 (PENDING | SENT | FAILED | null = 전체)") String status,
            @ToolParam(description = "페이지 번호 (0-based, 기본 0)") Integer page,
            @ToolParam(description = "페이지 크기 (기본 20, 최대 100)") Integer size
    ) {
        ApiKeyPrincipal principal = currentPrincipal();

        SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                fromDate != null ? LocalDateTime.parse(fromDate) : null,
                toDate != null ? LocalDateTime.parse(toDate) : null,
                channel != null ? SendChannel.valueOf(channel.toUpperCase().strip()) : null,
                callbackNumber,
                recipientNumber,
                status != null ? SendRequestStatus.valueOf(status.toUpperCase().strip()) : null
        );

        int p = page != null ? page : 0;
        int s = size != null ? Math.min(size, 100) : 20;

        return sendHistoryService.list(principal.memberId(), principal.apiKeyId(), params, p, s);
    }

    @Tool(description = """
            발송 이력 단건 상세를 조회한다. sendId (ULID 26자) 필수.
            필수 스코프: history:read
            응답: sendId, channel, callbackNumber, recipientNumbers, recipientCount, subject, messageBody, status, failReason, unitCost, totalCost, externalMsgId, requestedAt, createdAt, updatedAt
            (주의: routingMeta 등 내부 라우팅 정보는 포함되지 않음)
            """)
    public SendHistoryDto.DetailItem historyDetail(
            @ToolParam(description = "발송 ID (ULID 26자)") String sendId
    ) {
        ApiKeyPrincipal principal = currentPrincipal();
        return sendHistoryService.detail(principal.memberId(), principal.apiKeyId(), sendId);
    }

    private ApiKeyPrincipal currentPrincipal() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (auth instanceof ApiKeyPrincipal p) {
            return p;
        }
        throw new IllegalStateException("MCP 이력 도구는 API Key 인증이 필요합니다.");
    }
}
