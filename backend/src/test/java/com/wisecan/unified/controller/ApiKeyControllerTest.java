package com.wisecan.unified.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.*;
import com.wisecan.unified.dto.ApiKeyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.ApiKeyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiKeyController.class)
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    private Member stubMember() {
        Member member = Member.builder()
            .email("test@example.com")
            .password("pw")
            .name("테스터")
            .role(MemberRole.USER)
            .status(MemberStatus.ACTIVE)
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    private List<ApiKeyDto.ScopeInfo> stubScopes() {
        return List.of(
            new ApiKeyDto.ScopeInfo("SEND", "send", "모든 채널 발송"),
            new ApiKeyDto.ScopeInfo("HISTORY_READ", "history:read", "발송 이력 조회")
        );
    }

    // ─── 발급 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("POST /api/v1/api-keys - API 키 발급 성공")
    void create_success_returns201() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

        ApiKeyDto.CreateResponse response = new ApiKeyDto.CreateResponse(
            1L, "내 키", "wc_1234", "wc_1234abcdsecret", "ACTIVE", "TEST",
            stubScopes(), null, LocalDateTime.now()
        );
        given(apiKeyService.create(anyLong(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/api-keys")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApiKeyDto.CreateRequest("내 키"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.rawKey").value("wc_1234abcdsecret"))
            .andExpect(jsonPath("$.data.keyName").value("내 키"))
            .andExpect(jsonPath("$.data.keyType").value("TEST"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("POST /api/v1/api-keys - keyName 공백이면 400")
    void create_blankKeyName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/api-keys")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApiKeyDto.CreateRequest(""))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/api-keys - 인증 없으면 401")
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/api-keys")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ApiKeyDto.CreateRequest("키"))))
            .andExpect(status().isUnauthorized());
    }

    // ─── 목록 조회 ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("GET /api/v1/api-keys - 내 키 목록 조회 성공")
    void getMyKeys_success_returns200() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

        ApiKeyDto.Response keyResponse = new ApiKeyDto.Response(
            1L, "내 키", "wc_1234", "ACTIVE", "TEST",
            stubScopes(), null, List.of(), null, LocalDateTime.now()
        );
        given(apiKeyService.getMyKeys(1L)).willReturn(List.of(keyResponse));

        mockMvc.perform(get("/api/v1/api-keys"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].keyName").value("내 키"))
            .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data[0].keyType").value("TEST"));
    }

    // ─── 폐기 ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("PATCH /api/v1/api-keys/{id}/revoke - revoke 성공")
    void revoke_success_returns200() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

        mockMvc.perform(patch("/api/v1/api-keys/1/revoke").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("PATCH /api/v1/api-keys/{id}/revoke - 권한 없으면 400")
    void revoke_unauthorized_returns400() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        doThrow(new RuntimeException("해당 API 키에 대한 권한이 없습니다."))
            .when(apiKeyService).revoke(anyLong(), anyLong());

        mockMvc.perform(patch("/api/v1/api-keys/99/revoke").with(csrf()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ─── 재발급 ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("POST /api/v1/api-keys/{id}/rotate - 재발급 성공")
    void rotate_success_returns201() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

        ApiKeyDto.CreateResponse response = new ApiKeyDto.CreateResponse(
            2L, "내 키", "wc_5678", "wc_5678newsecret", "ACTIVE", "TEST",
            stubScopes(), null, LocalDateTime.now()
        );
        given(apiKeyService.rotate(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(post("/api/v1/api-keys/1/rotate").with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.rawKey").value("wc_5678newsecret"));
    }

    // ─── 스코프 수정 ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("PATCH /api/v1/api-keys/{id}/scopes - 스코프 수정 성공")
    void updateScopes_success_returns200() throws Exception {
        Member member = stubMember();
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

        ApiKeyDto.Response updated = new ApiKeyDto.Response(
            1L, "내 키", "wc_1234", "ACTIVE", "TEST",
            List.of(new ApiKeyDto.ScopeInfo("SEND", "send", "모든 채널 발송")),
            1000, List.of(), null, LocalDateTime.now()
        );
        given(apiKeyService.updateScopes(anyLong(), anyLong(), any())).willReturn(updated);

        ApiKeyDto.UpdateScopesRequest request = new ApiKeyDto.UpdateScopesRequest(
            Set.of(ApiKeyScope.SEND), 1000, null
        );

        mockMvc.perform(patch("/api/v1/api-keys/1/scopes")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.dailyLimit").value(1000));
    }

    // ─── 스코프 카탈로그 ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/api-keys/scopes/catalog - 인증 없이 카탈로그 조회 가능")
    void getScopeCatalog_noAuth_returns200() throws Exception {
        ApiKeyDto.PresetInfo presets = new ApiKeyDto.PresetInfo(
            List.of("send", "history:read"),
            List.of("send", "send:sms", "history:read"),
            List.of("history:read", "balance:read"),
            List.of("send", "history:read", "balance:read")
        );
        ApiKeyDto.ScopeCatalogResponse catalog = new ApiKeyDto.ScopeCatalogResponse(
            stubScopes(), presets
        );
        given(apiKeyService.getScopeCatalog()).willReturn(catalog);

        mockMvc.perform(get("/api/v1/api-keys/scopes/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.scopes").isArray())
            .andExpect(jsonPath("$.data.presets.test").isArray());
    }
}
