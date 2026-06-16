package com.wisecan.unified.exception;

public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Long id;

    public EntityNotFoundException(String displayName, Long id) {
        super(displayName + "을(를) 찾을 수 없습니다. (ID: " + id + ")");
        this.entityName = displayName;
        this.id = id;
    }

    /**
     * 메시지 기반 생성자 — 식별자 없이 사유 메시지만 전달하는 호출부용
     * (발송 게이트·결제·MCP 도구 등에서 사용).
     */
    public EntityNotFoundException(String message) {
        super(message);
        this.entityName = null;
        this.id = null;
    }

    public String getEntityName() { return entityName; }
    public Long getId() { return id; }
}
