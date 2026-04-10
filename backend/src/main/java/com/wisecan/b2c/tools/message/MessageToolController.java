package com.wisecan.b2c.tools.message;

import com.wisecan.b2c.dto.ApiResponse;
import com.wisecan.b2c.tools.message.dto.MessageToolDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 메시지 도구 프록시 컨트롤러.
 * /api/v1/tools/message/** 경로는 ApiKeyAuthFilter를 통해 인증된다.
 * Authentication.getName()에서 ApiKey의 memberId를 꺼내 서비스에 전달한다.
 */
@RestController
@RequestMapping("/api/v1/tools/message")
@RequiredArgsConstructor
public class MessageToolController {

    private final MessageToolService messageToolService;

    /**
     * POST /api/v1/tools/message/send — 메시지 발송.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageToolDto.SendResponse>> send(
        @RequestBody @Valid MessageToolDto.SendRequest request,
        Authentication authentication
    ) {
        Long apiKeyId = extractApiKeyId(authentication);
        MessageToolDto.SendResponse response = messageToolService.send(request, apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/tools/message/{msgId} — 메시지 결과 조회.
     */
    @GetMapping("/{msgId}")
    public ResponseEntity<ApiResponse<MessageToolDto.GetResponse>> getResult(
        @PathVariable String msgId,
        Authentication authentication
    ) {
        Long apiKeyId = extractApiKeyId(authentication);
        MessageToolDto.GetResponse response = messageToolService.getResult(msgId, apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/tools/message/search — 메시지 검색.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageToolDto.SearchResponse>>> search(
        @RequestParam(required = false) String channel,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        Long apiKeyId = extractApiKeyId(authentication);
        List<MessageToolDto.SearchResponse> result =
            messageToolService.search(channel, status, from, to, page, size, apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * ApiKeyAuthFilter가 SecurityContext에 주입한 principal에서 apiKeyId를 추출한다.
     * principal name = apiKey.getMember().getId().toString()
     * 주의: 여기서는 memberId를 apiKeyId로 재사용하는 임시 처리.
     * (실제 운영에서는 ApiKey 엔티티 ID를 principal에 담거나 별도 조회 필요)
     */
    private Long extractApiKeyId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("인증 정보가 없습니다");
        }
        return Long.parseLong(authentication.getName());
    }
}
