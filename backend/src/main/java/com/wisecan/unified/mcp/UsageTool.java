package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.dto.UsageDto;
import com.wisecan.unified.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * wsc.usage.summary — 사용량 요약 조회 도구 (RQ-MCP-011).
 *
 * 동등성 (§7.4): UsageService.getSummary() 를 직접 호출하여 CLI wsc usage 와 동일 결과.
 */
@Component
@RequiredArgsConstructor
public class UsageTool {

    private final UsageService usageService;

    @Tool(description = """
            API 사용량 요약을 조회한다 (전체 호출 수, 성공/실패 건수, 오늘 호출 수).
            필수 스코프: API Key 인증
            응답: { totalCalls, successCount, failCount, todayCalls }
            """)
    public UsageDto.SummaryResponse usageSummary(
            @ToolParam(description = "사용 안 함 — null 전달") String unused
    ) {
        ApiKeyPrincipal principal = currentPrincipal();
        return usageService.getSummary(principal.memberId());
    }

    private ApiKeyPrincipal currentPrincipal() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (auth instanceof ApiKeyPrincipal p) {
            return p;
        }
        throw new IllegalStateException("MCP 사용량 도구는 API Key 인증이 필요합니다.");
    }
}
