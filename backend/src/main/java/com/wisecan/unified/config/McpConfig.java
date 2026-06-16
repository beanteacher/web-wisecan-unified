package com.wisecan.unified.config;

import com.wisecan.unified.mcp.ApiKeyTool;
import com.wisecan.unified.mcp.BalanceTool;
import com.wisecan.unified.mcp.BrandTool;
import com.wisecan.unified.mcp.CallbackTool;
import com.wisecan.unified.mcp.GuideTool;
import com.wisecan.unified.mcp.HistoryTool;
import com.wisecan.unified.mcp.PingTool;
import com.wisecan.unified.mcp.SendTool;
import com.wisecan.unified.mcp.SnippetTool;
import com.wisecan.unified.mcp.TemplateTool;
import com.wisecan.unified.mcp.UsageTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 서버 도구 등록 설정 (W-303).
 *
 * <p>노출 도구 목록 (§7.3 RQ-MCP-001~014):</p>
 * <ol>
 *   <li>PingTool      — wsc.ping         : 헬스 체크 / 연결 확인</li>
 *   <li>GuideTool     — wsc.guide        : API 가이드 조회</li>
 *   <li>SnippetTool   — wsc.snippet      : 코드 스니펫 조회 (무인증)</li>
 *   <li>SendTool      — wsc.send.single  : 단건 발송</li>
 *   <li>SendTool      — wsc.send.bulk    : 다건 발송</li>
 *   <li>HistoryTool   — wsc.history.list : 발송 이력 목록</li>
 *   <li>HistoryTool   — wsc.history.detail : 발송 이력 상세</li>
 *   <li>CallbackTool  — wsc.callback.list : 발신번호 목록 조회</li>
 *   <li>ApiKeyTool    — wsc.key.list     : API 키 목록 조회</li>
 *   <li>BalanceTool   — wsc.balance.get  : 잔액 조회</li>
 *   <li>UsageTool     — wsc.usage.summary : 사용량 요약</li>
 *   <li>TemplateTool  — wsc.template.kakao.list : 카카오 템플릿 조회</li>
 *   <li>TemplateTool  — wsc.template.rcs.list   : RCS 템플릿 조회</li>
 *   <li>BrandTool     — wsc.brand.rcs.list       : RCS 브랜드 조회</li>
 * </ol>
 *
 * <p>비노출 정책 (§7.3): 키 발급/폐기, 발신번호 등록/삭제, 결제/환불/충전,
 * 키 스코프 변경은 MCP 도구로 등록하지 않는다.
 * AI Agent 자연어 추론으로 인한 우발 호출을 원천 차단한다.</p>
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            PingTool pingTool,
            GuideTool guideTool,
            SnippetTool snippetTool,
            SendTool sendTool,
            HistoryTool historyTool,
            CallbackTool callbackTool,
            ApiKeyTool apiKeyTool,
            BalanceTool balanceTool,
            UsageTool usageTool,
            TemplateTool templateTool,
            BrandTool brandTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        pingTool,
                        guideTool,
                        snippetTool,
                        sendTool,
                        historyTool,
                        callbackTool,
                        apiKeyTool,
                        balanceTool,
                        usageTool,
                        templateTool,
                        brandTool
                )
                .build();
    }
}
