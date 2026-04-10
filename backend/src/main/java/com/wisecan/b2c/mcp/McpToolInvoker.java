package com.wisecan.b2c.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * MCP 도구 호출 단일 진입점 인터페이스.
 * 타임아웃/에러는 McpException으로 변환하여 전파.
 */
public interface McpToolInvoker {

    /**
     * MCP 도구를 호출하고 결과를 반환한다.
     *
     * @param toolName 호출할 도구 이름
     * @param params   도구 파라미터 (key-value)
     * @return JSON-RPC 응답의 result 노드
     * @throws McpException 타임아웃, 프로세스 오류, 응답 파싱 실패 시
     */
    JsonNode invoke(String toolName, Map<String, Object> params);
}
