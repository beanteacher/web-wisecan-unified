package com.wisecan.b2c.tools.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 메시지 도구 프록시 API DTO.
 */
public class MessageToolDto {

    /**
     * 메시지 발송 요청.
     */
    public record SendRequest(
        @NotBlank(message = "채널은 필수입니다") String channel,
        @NotBlank(message = "수신자는 필수입니다") String recipient,
        @NotBlank(message = "메시지 내용은 필수입니다") String content,
        Map<String, Object> options
    ) {}

    /**
     * 메시지 발송 응답.
     */
    public record SendResponse(
        String messageId,
        String status,
        String channel,
        String recipient,
        String sentAt
    ) {}

    /**
     * 메시지 조회 응답.
     */
    public record GetResponse(
        String messageId,
        String channel,
        String recipient,
        String content,
        String status,
        String sentAt,
        String deliveredAt,
        Map<String, Object> metadata
    ) {}

    /**
     * 메시지 검색 응답 항목.
     */
    public record SearchResponse(
        String messageId,
        String channel,
        String recipient,
        String status,
        String sentAt,
        int responseTimeMs
    ) {}
}
