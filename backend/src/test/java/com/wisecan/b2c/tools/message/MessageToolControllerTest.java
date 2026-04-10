package com.wisecan.b2c.tools.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.b2c.config.JwtProvider;
import com.wisecan.b2c.mcp.McpException;
import com.wisecan.b2c.repository.ApiKeyRepository;
import com.wisecan.b2c.tools.message.dto.MessageToolDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageToolController.class)
class MessageToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageToolService messageToolService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @Test
    @WithMockUser(username = "10")
    @DisplayName("POST /api/v1/tools/message/send — 발송 성공 → 200")
    void send_success_returns200() throws Exception {
        MessageToolDto.SendRequest request = new MessageToolDto.SendRequest(
            "email", "user@example.com", "안녕하세요", null
        );
        MessageToolDto.SendResponse response = new MessageToolDto.SendResponse(
            "msg-1", "SENT", "email", "user@example.com", "2026-04-10T10:00:00"
        );
        given(messageToolService.send(any(), anyLong())).willReturn(response);

        mockMvc.perform(post("/api/v1/tools/message/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.messageId").value("msg-1"))
            .andExpect(jsonPath("$.data.status").value("SENT"));
    }

    @Test
    @WithMockUser(username = "10")
    @DisplayName("POST /api/v1/tools/message/send — 유효성 오류 → 400")
    void send_invalidRequest_returns400() throws Exception {
        // content 빈 값
        MessageToolDto.SendRequest request = new MessageToolDto.SendRequest(
            "email", "user@example.com", "", null
        );

        mockMvc.perform(post("/api/v1/tools/message/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "10")
    @DisplayName("GET /api/v1/tools/message/{msgId} — 조회 성공 → 200")
    void getResult_success_returns200() throws Exception {
        MessageToolDto.GetResponse response = new MessageToolDto.GetResponse(
            "msg-1", "email", "user@example.com", "안녕하세요",
            "DELIVERED", "2026-04-10T10:00:00", "2026-04-10T10:01:00", null
        );
        given(messageToolService.getResult(eq("msg-1"), anyLong())).willReturn(response);

        mockMvc.perform(get("/api/v1/tools/message/msg-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.messageId").value("msg-1"))
            .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    @WithMockUser(username = "10")
    @DisplayName("GET /api/v1/tools/message/search — 검색 성공 → 200")
    void search_success_returns200() throws Exception {
        MessageToolDto.SearchResponse item = new MessageToolDto.SearchResponse(
            "msg-1", "email", "user@example.com", "SENT", "2026-04-10T10:00:00", 120
        );
        given(messageToolService.search(any(), any(), any(), any(), anyInt(), anyInt(), anyLong()))
            .willReturn(List.of(item));

        mockMvc.perform(get("/api/v1/tools/message/search")
                .param("channel", "email")
                .param("status", "SENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].messageId").value("msg-1"))
            .andExpect(jsonPath("$.data[0].channel").value("email"));
    }

    @Test
    @WithMockUser(username = "10")
    @DisplayName("GET /api/v1/tools/message/{msgId} — MCP 오류 → 400")
    void getResult_mcpError_returns400() throws Exception {
        given(messageToolService.getResult(eq("bad-id"), anyLong()))
            .willThrow(new McpException("MCP 연결 실패"));

        mockMvc.perform(get("/api/v1/tools/message/bad-id"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
