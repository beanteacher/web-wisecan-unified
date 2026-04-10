package com.wisecan.b2c.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP 서버와 JSON-RPC 통신을 담당한다.
 * 요청 직렬화 / 응답 파싱을 처리하며, 응답은 JsonNode로 관대하게 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpClient {

    private final McpProcessManager processManager;
    private final ObjectMapper objectMapper;
    private final AtomicLong idCounter = new AtomicLong(1);

    /**
     * JSON-RPC 2.0 형식으로 tools/call 요청을 보내고 result 노드를 반환한다.
     *
     * @param toolName MCP 도구 이름
     * @param params   파라미터 맵
     * @param timeoutMs 응답 대기 타임아웃 (밀리초)
     * @return result JsonNode
     * @throws McpException 통신 오류, 파싱 오류, 에러 응답 시
     */
    public JsonNode callTool(String toolName, Map<String, Object> params, long timeoutMs) {
        Process proc = processManager.startProcess();

        long requestId = idCounter.getAndIncrement();
        String requestJson = buildRequest(requestId, toolName, params);

        log.debug("MCP 요청 [id={}]: {}", requestId, requestJson);

        try {
            // 요청 전송
            OutputStream os = proc.getOutputStream();
            os.write((requestJson + "\n").getBytes(StandardCharsets.UTF_8));
            os.flush();

            // 응답 수신 (타임아웃 적용)
            String responseLine = readLineWithTimeout(proc, timeoutMs);
            log.debug("MCP 응답 [id={}]: {}", requestId, responseLine);

            return parseResponse(responseLine, requestId);

        } catch (IOException e) {
            throw new McpException("MCP 통신 오류: " + e.getMessage(), e);
        }
    }

    private String buildRequest(long id, String toolName, Map<String, Object> params) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("jsonrpc", "2.0");
            root.put("id", id);
            root.put("method", "tools/call");

            ObjectNode paramsNode = objectMapper.createObjectNode();
            paramsNode.put("name", toolName);
            paramsNode.set("arguments", objectMapper.valueToTree(params));
            root.set("params", paramsNode);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new McpException("MCP 요청 직렬화 실패", e);
        }
    }

    private String readLineWithTimeout(Process proc, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder sb = new StringBuilder();
        while (System.currentTimeMillis() < deadline) {
            if (reader.ready()) {
                int ch = reader.read();
                if (ch == -1 || ch == '\n') {
                    String line = sb.toString().trim();
                    if (!line.isEmpty()) {
                        return line;
                    }
                    sb.setLength(0);
                } else {
                    sb.append((char) ch);
                }
            } else {
                if (!proc.isAlive()) {
                    throw new McpException("MCP 프로세스가 응답 전에 종료되었습니다");
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new McpException("MCP 응답 대기 중 인터럽트 발생");
                }
            }
        }
        throw new McpException("MCP 응답 타임아웃 (" + timeoutMs + "ms)");
    }

    private JsonNode parseResponse(String json, long expectedId) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // 에러 응답 처리
            if (root.has("error")) {
                JsonNode error = root.get("error");
                String msg = error.has("message") ? error.get("message").asText() : error.toString();
                throw new McpException("MCP 에러 응답: " + msg);
            }

            // result 노드 반환
            if (!root.has("result")) {
                throw new McpException("MCP 응답에 result 필드가 없습니다: " + json);
            }

            return root.get("result");

        } catch (McpException e) {
            throw e;
        } catch (Exception e) {
            throw new McpException("MCP 응답 파싱 실패: " + json, e);
        }
    }
}
