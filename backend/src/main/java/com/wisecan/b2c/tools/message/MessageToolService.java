package com.wisecan.b2c.tools.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.b2c.domain.ApiKey;
import com.wisecan.b2c.domain.ApiUsage;
import com.wisecan.b2c.domain.UsageStatus;
import com.wisecan.b2c.mcp.McpException;
import com.wisecan.b2c.mcp.McpToolInvoker;
import com.wisecan.b2c.repository.ApiKeyRepository;
import com.wisecan.b2c.repository.ApiUsageRepository;
import com.wisecan.b2c.tools.message.dto.MessageToolDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메시지 도구 프록시 서비스.
 * McpToolInvoker를 통해 MCP 서버에 요청을 위임하고 ApiUsage를 기록한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageToolService {

    private final McpToolInvoker mcpToolInvoker;
    private final ApiUsageRepository apiUsageRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;

    /**
     * 메시지 발송.
     *
     * @param request   발송 요청 DTO
     * @param apiKeyId  인증된 API Key ID (UsageRepository 기록용)
     * @return 발송 결과
     */
    public MessageToolDto.SendResponse send(MessageToolDto.SendRequest request, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.getReferenceById(apiKeyId);
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("channel", request.channel());
            params.put("recipient", request.recipient());
            params.put("content", request.content());
            if (request.options() != null) {
                params.putAll(request.options());
            }

            JsonNode result = mcpToolInvoker.invoke("send_message", params);
            int elapsed = (int) (System.currentTimeMillis() - start);

            recordUsage(apiKey, "send_message", UsageStatus.SUCCESS, elapsed, null);

            return new MessageToolDto.SendResponse(
                getTextOrNull(result, "messageId"),
                getTextOrNull(result, "status"),
                request.channel(),
                request.recipient(),
                getTextOrNull(result, "sentAt")
            );

        } catch (McpException e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            recordUsage(apiKey, "send_message", UsageStatus.FAIL, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * 메시지 결과 조회.
     *
     * @param msgId    조회할 메시지 ID
     * @param apiKeyId 인증된 API Key ID
     * @return 조회 결과
     */
    @Transactional(readOnly = true)
    public MessageToolDto.GetResponse getResult(String msgId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.getReferenceById(apiKeyId);
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> params = Map.of("messageId", msgId);
            JsonNode result = mcpToolInvoker.invoke("get_message_result", params);
            int elapsed = (int) (System.currentTimeMillis() - start);

            recordUsage(apiKey, "get_message_result", UsageStatus.SUCCESS, elapsed, null);

            return new MessageToolDto.GetResponse(
                getTextOrNull(result, "messageId"),
                getTextOrNull(result, "channel"),
                getTextOrNull(result, "recipient"),
                getTextOrNull(result, "content"),
                getTextOrNull(result, "status"),
                getTextOrNull(result, "sentAt"),
                getTextOrNull(result, "deliveredAt"),
                result.has("metadata") ? objectMapper.convertValue(result.get("metadata"), Map.class) : null
            );

        } catch (McpException e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            recordUsage(apiKey, "get_message_result", UsageStatus.FAIL, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * 메시지 검색.
     *
     * @param channel   채널 필터 (nullable)
     * @param status    상태 필터 (nullable)
     * @param from      시작 날짜 (nullable)
     * @param to        종료 날짜 (nullable)
     * @param page      페이지 번호
     * @param size      페이지 크기
     * @param apiKeyId  인증된 API Key ID
     * @return 검색 결과 목록
     */
    @Transactional(readOnly = true)
    public List<MessageToolDto.SearchResponse> search(
        String channel, String status, String from, String to,
        int page, int size, Long apiKeyId
    ) {
        ApiKey apiKey = apiKeyRepository.getReferenceById(apiKeyId);
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> params = new HashMap<>();
            if (channel != null) params.put("channel", channel);
            if (status != null) params.put("status", status);
            if (from != null) params.put("from", from);
            if (to != null) params.put("to", to);
            params.put("page", page);
            params.put("size", size);

            JsonNode result = mcpToolInvoker.invoke("search_messages", params);
            int elapsed = (int) (System.currentTimeMillis() - start);

            recordUsage(apiKey, "search_messages", UsageStatus.SUCCESS, elapsed, null);

            if (!result.isArray()) {
                return List.of();
            }

            return objectMapper.convertValue(
                result,
                objectMapper.getTypeFactory().constructCollectionType(List.class, MessageToolDto.SearchResponse.class)
            );

        } catch (McpException e) {
            int elapsed = (int) (System.currentTimeMillis() - start);
            recordUsage(apiKey, "search_messages", UsageStatus.FAIL, elapsed, e.getMessage());
            throw e;
        }
    }

    private void recordUsage(ApiKey apiKey, String toolName, UsageStatus status, int responseTimeMs, String errorMessage) {
        ApiUsage usage = ApiUsage.builder()
            .apiKey(apiKey)
            .toolName(toolName)
            .status(status)
            .responseTimeMs(responseTimeMs)
            .errorMessage(errorMessage)
            .build();
        apiUsageRepository.save(usage);
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node != null && node.has(field) && !node.get(field).isNull()
            ? node.get(field).asText()
            : null;
    }
}
