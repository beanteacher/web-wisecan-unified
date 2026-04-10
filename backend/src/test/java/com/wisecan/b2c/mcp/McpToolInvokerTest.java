package com.wisecan.b2c.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolInvokerTest {

    @Mock
    private McpClient mcpClient;

    @Mock
    private McpProcessManager processManager;

    private McpToolInvokerImpl invoker;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        invoker = new McpToolInvokerImpl(mcpClient, processManager);
        // timeoutMs 필드를 리플렉션으로 주입
        setField(invoker, "timeoutMs", 5000L);
    }

    @Test
    @DisplayName("정상 호출 — result 노드 반환")
    void invoke_success_returnsResult() throws Exception {
        // given
        JsonNode resultNode = objectMapper.readTree("{\"content\":\"hello\"}");
        given(mcpClient.callTool(eq("send_message"), any(), anyLong())).willReturn(resultNode);

        // when
        JsonNode result = invoker.invoke("send_message", Map.of("text", "hello"));

        // then
        assertThat(result).isNotNull();
        assertThat(result.get("content").asText()).isEqualTo("hello");
        then(mcpClient).should().callTool(eq("send_message"), any(), anyLong());
    }

    @Test
    @DisplayName("에러 응답 — McpException 전파")
    void invoke_mcpError_throwsMcpException() {
        // given
        given(mcpClient.callTool(any(), any(), anyLong()))
            .willThrow(new McpException("MCP 에러 응답: tool not found"));

        // when / then
        assertThatThrownBy(() -> invoker.invoke("unknown_tool", Map.of()))
            .isInstanceOf(McpException.class)
            .hasMessageContaining("MCP 에러 응답");
    }

    @Test
    @DisplayName("타임아웃 — McpException 전파")
    void invoke_timeout_throwsMcpException() {
        // given
        given(mcpClient.callTool(any(), any(), anyLong()))
            .willThrow(new McpException("MCP 응답 타임아웃 (5000ms)"));

        // when / then
        assertThatThrownBy(() -> invoker.invoke("send_message", Map.of("text", "hi")))
            .isInstanceOf(McpException.class)
            .hasMessageContaining("타임아웃");
    }

    @Test
    @DisplayName("빈 파라미터 — 정상 호출")
    void invoke_emptyParams_success() throws Exception {
        // given
        JsonNode resultNode = objectMapper.readTree("{\"status\":\"ok\"}");
        given(mcpClient.callTool(eq("list_tools"), eq(Map.of()), anyLong())).willReturn(resultNode);

        // when
        JsonNode result = invoker.invoke("list_tools", Map.of());

        // then
        assertThat(result.get("status").asText()).isEqualTo("ok");
    }

    // 리플렉션 유틸
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
