package com.wisecan.unified.controller.sendernumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.sendernumber.CallbackDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.service.TokenBlacklistService;
import com.wisecan.unified.service.sendernumber.CallbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CallbackController.class)
class CallbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CallbackService callbackService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    // ── §4.1 SELF_MOBILE 즉시 등록 ──────────────────────────────

    @Test
    @DisplayName("SELF_MOBILE 등록 성공 — 201 Created, status=REGISTERED")
    @WithMockUser(username = "user@test.com")
    void register_selfMobile_success() throws Exception {
        CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
            "01012345678", CallbackRegisterType.SELF_MOBILE, "고객 문자 발송용"
        );
        CallbackDto.RegisterResponse resp = new CallbackDto.RegisterResponse(
            1L, "01012345678", CallbackStatus.REGISTERED, "발신번호가 즉시 등록되었습니다."
        );

        given(callbackService.register(eq("user@test.com"), any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    // ── §4.2 SELF_LANDLINE ───────────────────────────────────────

    @Test
    @DisplayName("SELF_LANDLINE 등록 성공 — 201 Created, status=REGISTERED")
    @WithMockUser(username = "user@test.com")
    void register_selfLandline_success() throws Exception {
        CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
            "0215551234", CallbackRegisterType.SELF_LANDLINE, "사무실 대표번호"
        );
        CallbackDto.RegisterResponse resp = new CallbackDto.RegisterResponse(
            2L, "0215551234", CallbackStatus.REGISTERED, "발신번호가 즉시 등록되었습니다."
        );

        given(callbackService.register(eq("user@test.com"), any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("REGISTERED"));
    }

    // ── §4.3 EMPLOYEE 심사형 ─────────────────────────────────────

    @Test
    @DisplayName("EMPLOYEE 등록 — 201 Created, status=SUBMITTED")
    @WithMockUser(username = "master@test.com")
    void register_employee_submitted() throws Exception {
        CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
            "0215559999", CallbackRegisterType.EMPLOYEE, "임직원 내선번호"
        );
        CallbackDto.RegisterResponse resp = new CallbackDto.RegisterResponse(
            3L, "0215559999", CallbackStatus.SUBMITTED, "등록 신청이 접수되었습니다. 운영자 심사 후 승인됩니다."
        );

        given(callbackService.register(eq("master@test.com"), any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    // ── §4.3 CORP_REP 심사형 ────────────────────────────────────

    @Test
    @DisplayName("CORP_REP 등록 — 201 Created, status=SUBMITTED")
    @WithMockUser(username = "master@test.com")
    void register_corpRep_submitted() throws Exception {
        CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
            "025550000", CallbackRegisterType.CORP_REP, "법인 대표번호"
        );
        CallbackDto.RegisterResponse resp = new CallbackDto.RegisterResponse(
            4L, "025550000", CallbackStatus.SUBMITTED, "등록 신청이 접수되었습니다. 운영자 심사 후 승인됩니다."
        );

        given(callbackService.register(eq("master@test.com"), any())).willReturn(resp);

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    // ── 중복 등록 방지 ────────────────────────────────────────────

    @Test
    @DisplayName("이미 활성 등록 존재 — 409 Conflict")
    @WithMockUser(username = "user@test.com")
    void register_duplicate_conflict() throws Exception {
        CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
            "01012345678", CallbackRegisterType.SELF_MOBILE, "중복"
        );

        given(callbackService.register(any(), any()))
            .willThrow(new IllegalStateException("이미 등록되었거나 심사 중인 발신번호입니다"));

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── §4.4 삭제 ────────────────────────────────────────────────

    @Test
    @DisplayName("발신번호 삭제 성공 — 200 OK")
    @WithMockUser(username = "user@test.com")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/callbacks/1")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 발신번호 삭제 — 404 Not Found")
    @WithMockUser(username = "user@test.com")
    void delete_notFound() throws Exception {
        doThrow(new EntityNotFoundException("발신번호를 찾을 수 없습니다: 99"))
            .when(callbackService).delete(eq("user@test.com"), eq(99L));

        mockMvc.perform(delete("/api/v1/callbacks/99")
                .with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── 목록 조회 ─────────────────────────────────────────────────

    @Test
    @DisplayName("발신번호 목록 조회 — 200 OK")
    @WithMockUser(username = "user@test.com")
    void list_success() throws Exception {
        given(callbackService.list("user@test.com")).willReturn(List.of());

        mockMvc.perform(get("/api/v1/callbacks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }

    // ── 유효성 검증 ───────────────────────────────────────────────

    @Test
    @DisplayName("phoneNumber 누락 — 400 Bad Request")
    @WithMockUser(username = "user@test.com")
    void register_noPhone_badRequest() throws Exception {
        String body = "{\"registerType\":\"SELF_MOBILE\",\"description\":\"테스트\"}";

        mockMvc.perform(post("/api/v1/callbacks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }
}
