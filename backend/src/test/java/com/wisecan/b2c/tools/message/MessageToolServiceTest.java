package com.wisecan.b2c.tools.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.b2c.domain.ApiKey;
import com.wisecan.b2c.domain.ApiKeyStatus;
import com.wisecan.b2c.domain.ApiUsage;
import com.wisecan.b2c.domain.Member;
import com.wisecan.b2c.domain.MemberRole;
import com.wisecan.b2c.domain.MemberStatus;
import com.wisecan.b2c.domain.UsageStatus;
import com.wisecan.b2c.mcp.McpException;
import com.wisecan.b2c.mcp.McpToolInvoker;
import com.wisecan.b2c.repository.ApiKeyRepository;
import com.wisecan.b2c.repository.ApiUsageRepository;
import com.wisecan.b2c.tools.message.dto.MessageToolDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MessageToolServiceTest {

    @Mock
    private McpToolInvoker mcpToolInvoker;

    @Mock
    private ApiUsageRepository apiUsageRepository;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private MessageToolService service;
    private ObjectMapper objectMapper;
    private ApiKey stubApiKey;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new MessageToolService(mcpToolInvoker, apiUsageRepository, apiKeyRepository, objectMapper);

        Member member = Member.builder()
            .email("test@test.com").password("pw").name("테스터")
            .role(MemberRole.USER).status(MemberStatus.ACTIVE).build();
        ReflectionTestUtils.setField(member, "id", 1L);

        stubApiKey = ApiKey.builder()
            .member(member).keyName("테스트키").keyPrefix("wc").keyHash("hash")
            .status(ApiKeyStatus.ACTIVE).build();
        ReflectionTestUtils.setField(stubApiKey, "id", 10L);

        given(apiKeyRepository.getReferenceById(10L)).willReturn(stubApiKey);
    }

    @Test
    @DisplayName("send 성공 — SendResponse 반환 + ApiUsage SUCCESS 기록")
    void send_success_recordsUsageAndReturnsResponse() throws Exception {
        JsonNode result = objectMapper.readTree(
            "{\"messageId\":\"msg-1\",\"status\":\"SENT\",\"sentAt\":\"2026-04-10T10:00:00\"}"
        );
        given(mcpToolInvoker.invoke(eq("send_message"), any())).willReturn(result);

        MessageToolDto.SendRequest request = new MessageToolDto.SendRequest(
            "email", "user@example.com", "안녕하세요", null
        );

        MessageToolDto.SendResponse response = service.send(request, 10L);

        assertThat(response.messageId()).isEqualTo("msg-1");
        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.channel()).isEqualTo("email");

        ArgumentCaptor<ApiUsage> captor = ArgumentCaptor.forClass(ApiUsage.class);
        then(apiUsageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UsageStatus.SUCCESS);
        assertThat(captor.getValue().getToolName()).isEqualTo("send_message");
    }

    @Test
    @DisplayName("send 실패 — McpException 전파 + ApiUsage FAIL 기록")
    void send_mcpError_recordsFailAndRethrows() {
        given(mcpToolInvoker.invoke(eq("send_message"), any()))
            .willThrow(new McpException("MCP 연결 실패"));

        MessageToolDto.SendRequest request = new MessageToolDto.SendRequest(
            "sms", "010-1234-5678", "테스트", null
        );

        assertThatThrownBy(() -> service.send(request, 10L))
            .isInstanceOf(McpException.class)
            .hasMessageContaining("MCP 연결 실패");

        ArgumentCaptor<ApiUsage> captor = ArgumentCaptor.forClass(ApiUsage.class);
        then(apiUsageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UsageStatus.FAIL);
        assertThat(captor.getValue().getErrorMessage()).contains("MCP 연결 실패");
    }

    @Test
    @DisplayName("getResult 성공 — GetResponse 반환 + ApiUsage SUCCESS 기록")
    void getResult_success_recordsUsageAndReturnsResponse() throws Exception {
        JsonNode result = objectMapper.readTree(
            "{\"messageId\":\"msg-1\",\"channel\":\"email\",\"recipient\":\"u@e.com\"," +
            "\"content\":\"안녕\",\"status\":\"DELIVERED\",\"sentAt\":\"2026-04-10T10:00:00\",\"deliveredAt\":\"2026-04-10T10:01:00\"}"
        );
        given(mcpToolInvoker.invoke(eq("get_message_result"), any())).willReturn(result);

        MessageToolDto.GetResponse response = service.getResult("msg-1", 10L);

        assertThat(response.messageId()).isEqualTo("msg-1");
        assertThat(response.status()).isEqualTo("DELIVERED");

        ArgumentCaptor<ApiUsage> captor = ArgumentCaptor.forClass(ApiUsage.class);
        then(apiUsageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UsageStatus.SUCCESS);
        assertThat(captor.getValue().getToolName()).isEqualTo("get_message_result");
    }

    @Test
    @DisplayName("search 성공 — 결과 목록 반환 + ApiUsage SUCCESS 기록")
    void search_success_recordsUsageAndReturnsList() throws Exception {
        JsonNode result = objectMapper.readTree(
            "[{\"messageId\":\"msg-1\",\"channel\":\"email\",\"recipient\":\"u@e.com\"," +
            "\"status\":\"SENT\",\"sentAt\":\"2026-04-10T10:00:00\",\"responseTimeMs\":100}]"
        );
        given(mcpToolInvoker.invoke(eq("search_messages"), any())).willReturn(result);

        var responses = service.search("email", "SENT", null, null, 0, 20, 10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).messageId()).isEqualTo("msg-1");

        ArgumentCaptor<ApiUsage> captor = ArgumentCaptor.forClass(ApiUsage.class);
        then(apiUsageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UsageStatus.SUCCESS);
        assertThat(captor.getValue().getToolName()).isEqualTo("search_messages");
    }

    @Test
    @DisplayName("search 실패 — McpException 전파 + ApiUsage FAIL 기록")
    void search_mcpError_recordsFailAndRethrows() {
        given(mcpToolInvoker.invoke(eq("search_messages"), any()))
            .willThrow(new McpException("타임아웃"));

        assertThatThrownBy(() -> service.search(null, null, null, null, 0, 20, 10L))
            .isInstanceOf(McpException.class);

        ArgumentCaptor<ApiUsage> captor = ArgumentCaptor.forClass(ApiUsage.class);
        then(apiUsageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UsageStatus.FAIL);
    }
}
