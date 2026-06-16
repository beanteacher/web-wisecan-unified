package com.wisecan.unified.controller.cs;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.cs.ChatbotDto;
import com.wisecan.unified.service.cs.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cs/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇 FAQ 기반 질의응답 (공개 API — 비로그인도 사용 가능).
     * 매칭 실패 시 1:1 문의 유도 메시지를 반환한다.
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatbotDto.QueryResponse>> query(
            @RequestBody @Valid ChatbotDto.QueryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatbotService.query(request.question())));
    }
}
