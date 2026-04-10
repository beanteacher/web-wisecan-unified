package com.wisecan.b2c.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * McpToolInvoker 구현체.
 * McpClient를 통해 MCP 서버에 도구 호출을 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolInvokerImpl implements McpToolInvoker {

    private final McpClient mcpClient;
    private final McpProcessManager processManager;

    @Value("${wisecan.mcp.timeout-ms:10000}")
    private long timeoutMs;

    @Override
    public JsonNode invoke(String toolName, Map<String, Object> params) {
        log.info("MCP 도구 호출: toolName={}, params={}", toolName, params);
        try {
            JsonNode result = mcpClient.callTool(toolName, params, timeoutMs);
            log.info("MCP 도구 호출 성공: toolName={}", toolName);
            return result;
        } catch (McpException e) {
            log.error("MCP 도구 호출 실패: toolName={}, error={}", toolName, e.getMessage());
            throw e;
        }
    }
}
