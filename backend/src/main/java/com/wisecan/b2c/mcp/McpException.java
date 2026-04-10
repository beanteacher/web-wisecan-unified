package com.wisecan.b2c.mcp;

/**
 * MCP 연동 중 발생하는 모든 예외의 최상위 클래스.
 * GlobalExceptionHandler의 RuntimeException 핸들러로 400 응답 처리됨.
 */
public class McpException extends RuntimeException {

    public McpException(String message) {
        super(message);
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
