package com.wisecan.unified.controller.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.billing.ChargeStatus;
import com.wisecan.unified.domain.billing.ChargeType;
import com.wisecan.unified.domain.billing.PaymentMethodType;
import com.wisecan.unified.dto.billing.ChargeDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.service.TokenBlacklistService;
import com.wisecan.unified.service.billing.BalanceQueryService;
import com.wisecan.unified.service.billing.ChargeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChargeController.class)
class ChargeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ChargeService chargeService;
    @MockBean BalanceQueryService balanceQueryService;
    @MockBean JwtProvider jwtProvider;
    @MockBean ApiKeyRepository apiKeyRepository;
    @MockBean TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("POST /billing/charge — 충전 성공 200 OK")
    @WithMockUser
    void charge_success_returns200() throws Exception {
        ChargeDto.Response response = new ChargeDto.Response(
                1L, 100L, PaymentMethodType.CREDIT_CARD, 10_000L,
                ChargeType.MANUAL, ChargeStatus.SUCCESS, "STUB-TX-001",
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(chargeService.charge(anyLong(), anyString(), any(ChargeDto.CreateRequest.class)))
                .willReturn(response);

        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        mockMvc.perform(post("/billing/charge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.pgTxId").value("STUB-TX-001"));
    }

    @Test
    @DisplayName("POST /billing/charge — paymentMethodId 누락 시 400")
    @WithMockUser
    void charge_missingPaymentMethodId_returns400() throws Exception {
        String body = "{\"amount\": 10000}";

        mockMvc.perform(post("/billing/charge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /billing/charge — amount 1000 미만 시 400")
    @WithMockUser
    void charge_amountTooSmall_returns400() throws Exception {
        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 500L);

        mockMvc.perform(post("/billing/charge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /billing/charge — POSTPAID 회원 BillingException → 400")
    @WithMockUser
    void charge_postpaidMember_returns400() throws Exception {
        given(chargeService.charge(anyLong(), anyString(), any()))
                .willThrow(new BillingException("후불 정산 회원은 수동 충전을 이용할 수 없습니다."));

        ChargeDto.CreateRequest request = new ChargeDto.CreateRequest(1L, 10_000L);

        mockMvc.perform(post("/billing/charge")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("후불 정산")));
    }

    @Test
    @DisplayName("GET /billing/balance — 잔액 조회 200 OK")
    @WithMockUser
    void balance_success_returns200() throws Exception {
        ChargeDto.BalanceResponse response = new ChargeDto.BalanceResponse(100L, 30_000L, true);
        given(balanceQueryService.getBalance(anyLong())).willReturn(response);

        mockMvc.perform(get("/billing/balance").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBalance").value(30_000));
    }

    @Test
    @DisplayName("GET /billing/balance — 미인증 요청 401")
    void balance_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/billing/balance"))
                .andExpect(status().isUnauthorized());
    }
}
