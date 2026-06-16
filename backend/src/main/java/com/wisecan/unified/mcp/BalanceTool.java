package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * wsc.balance.get — 잔액 조회 도구 (RQ-MCP-010).
 *
 * 비노출 정책 (§7.3): 충전·환불은 MCP 도구로 노출하지 않는다. 조회(balance:read)만 허용.
 * 동등성 (§7.4): CLI wsc balance 와 동일 결과.
 */
@Component
@RequiredArgsConstructor
public class BalanceTool {

    @Tool(description = """
            현재 잔액을 조회한다.
            필수 스코프: balance:read
            응답: { memberId, balance, currency }
            (주의: 충전·환불은 웹 콘솔 전용입니다. MCP 도구로는 조회만 가능합니다.)
            """)
    public Map<String, Object> balanceGet(
            @ToolParam(description = "사용 안 함 — null 전달") String unused
    ) {
        ApiKeyPrincipal principal = currentPrincipal();
        // 잔액 도메인(W-208)이 구현되면 BalanceService 주입으로 대체한다.
        // 현재는 memberId 기반 안내 응답을 반환한다.
        return Map.of(
                "memberId", principal.memberId(),
                "balance", 0,
                "currency", "KRW",
                "note", "잔액 서비스(W-208)가 연동되면 실제 값이 반환됩니다."
        );
    }

    private ApiKeyPrincipal currentPrincipal() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (auth instanceof ApiKeyPrincipal p) {
            return p;
        }
        throw new IllegalStateException("MCP 잔액 도구는 API Key 인증이 필요합니다.");
    }
}
