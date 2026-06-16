package com.wisecan.unified.controller.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.config.JwtProvider;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.dispatch.WebSendDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.dispatch.WebSendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebSendController 슬라이스 테스트 (W-206).
 *
 * <p>@WebMvcTest로 컨트롤러 레이어만 로드. WebSendService는 @MockitoBean.</p>
 * <p>UserPrincipal은 SecurityMockMvcRequestPostProcessors.user()로 주입.</p>
 */
@WebMvcTest(WebSendController.class)
class WebSendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WebSendService webSendService;

    // SecurityConfig 의존 빈 — WebMvcTest 컨텍스트에 필요
    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    // ── 픽스처 ────────────────────────────────────────────────────────

    private UserPrincipal principal() {
        return new UserPrincipal(1L, "test@example.com", Set.of("ROLE_USER"));
    }

    // UserPrincipal 을 인증 주체로 주입하는 인증 토큰 post-processor (@AuthenticationPrincipal 대응)
    private RequestPostProcessor auth() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal(), null, List.of()));
    }

    private WebSendDto.AcceptResponse stubAccept(int recipientCount) {
        return new WebSendDto.AcceptResponse(
                "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                SendRequestStatus.PENDING,
                recipientCount,
                (long) recipientCount * 20L,
                null
        );
    }

    private WebSendDto.AcceptResponse stubAcceptScheduled(LocalDateTime scheduledAt) {
        return new WebSendDto.AcceptResponse(
                "01ARZ3NDEKTSV4RRFFQ69G5FAV",
                SendRequestStatus.PENDING,
                1,
                20L,
                scheduledAt
        );
    }

    // ── POST /console/send (단건 발송) ────────────────────────────────

    @Test
    @DisplayName("POST /console/send — SMS 단건 발송 성공: 200 + sendId 반환")
    void send_smsSingle_returns200WithSendId() throws Exception {
        given(webSendService.send(eq(1L), any())).willReturn(stubAccept(1));

        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "안녕하세요", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sendId").value("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.recipientCount").value(1));
    }

    @Test
    @DisplayName("POST /console/send — 인증 없으면 401")
    void send_noAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "테스트", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /console/send — callbackNumber 공백: 400")
    void send_blankCallbackNumber_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "", List.of("01099999999"), SendChannel.SMS,
                null, "테스트", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /console/send — recipientNumbers 빈 리스트: 400")
    void send_emptyRecipients_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "01012345678", List.of(), SendChannel.SMS,
                null, "테스트", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /console/send — messageBody 공백: 400")
    void send_blankMessageBody_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /console/send — 상용 API Key 없으면 404")
    void send_noProductionKey_returns404() throws Exception {
        given(webSendService.send(eq(1L), any()))
                .willThrow(new EntityNotFoundException("활성 상용 API Key가 없습니다"));

        String body = objectMapper.writeValueAsString(new WebSendDto.SingleRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "테스트", false, null, null
        ));

        mockMvc.perform(post("/console/send")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST /console/send/bulk (일괄 발송) ───────────────────────────

    @Test
    @DisplayName("POST /console/send/bulk — 100명 일괄 발송 성공: 200 + recipientCount=100")
    void sendBulk_hundredRecipients_returns200() throws Exception {
        given(webSendService.sendBulk(eq(1L), any())).willReturn(stubAccept(100));

        List<String> recipients = java.util.stream.IntStream.rangeClosed(1, 100)
                .mapToObj(i -> String.format("0101%07d", i))
                .toList();
        String body = objectMapper.writeValueAsString(new WebSendDto.BulkRequest(
                "01012345678", SendChannel.SMS, null, "일괄 발송", false, null, null, recipients
        ));

        mockMvc.perform(post("/console/send/bulk")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipientCount").value(100));
    }

    @Test
    @DisplayName("POST /console/send/bulk — 인증 없으면 401")
    void sendBulk_noAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.BulkRequest(
                "01012345678", SendChannel.SMS, null, "일괄", false, null, null,
                List.of("01099999999")
        ));

        mockMvc.perform(post("/console/send/bulk")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /console/send/bulk — recipientNumbers 빈 리스트: 400")
    void sendBulk_emptyRecipients_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new WebSendDto.BulkRequest(
                "01012345678", SendChannel.SMS, null, "일괄", false, null, null, List.of()
        ));

        mockMvc.perform(post("/console/send/bulk")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── POST /console/send/scheduled (예약 발송) ──────────────────────

    @Test
    @DisplayName("POST /console/send/scheduled — 미래 시각 예약 성공: 200 + scheduledAt 포함")
    void sendScheduled_futureDatetime_returns200WithScheduledAt() throws Exception {
        LocalDateTime future = LocalDateTime.now().plusHours(3);
        given(webSendService.sendScheduled(eq(1L), any())).willReturn(stubAcceptScheduled(future));

        String body = objectMapper.writeValueAsString(new WebSendDto.ScheduledRequest(
                "01012345678", List.of("01099999999"), SendChannel.SMS,
                null, "예약 메시지", false, null, null, future
        ));

        mockMvc.perform(post("/console/send/scheduled")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduledAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /console/send/scheduled — scheduledAt 누락: 400")
    void sendScheduled_nullScheduledAt_returns400() throws Exception {
        // scheduledAt=null 은 JSON 직렬화 시 null로 전송
        String body = """
                {
                  "callbackNumber":"01012345678",
                  "recipientNumbers":["01099999999"],
                  "channel":"SMS",
                  "messageBody":"테스트",
                  "isAdvertisement":false,
                  "scheduledAt":null
                }
                """;

        mockMvc.perform(post("/console/send/scheduled")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /console/send/scheduled — 인증 없으면 401")
    void sendScheduled_noAuth_returns401() throws Exception {
        String body = """
                {
                  "callbackNumber":"01012345678",
                  "recipientNumbers":["01099999999"],
                  "channel":"SMS",
                  "messageBody":"테스트",
                  "isAdvertisement":false,
                  "scheduledAt":"2099-12-31T09:00:00"
                }
                """;

        mockMvc.perform(post("/console/send/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /console/send/scheduled (예약 목록) ───────────────────────

    @Test
    @DisplayName("GET /console/send/scheduled — 예약 목록 조회 성공: 200 + Page 반환")
    void listScheduled_returns200WithPage() throws Exception {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        WebSendDto.ScheduledSummary summary = new WebSendDto.ScheduledSummary(
                "01ARZ3NDEKTSV4RRFFQ69G5FAV", SendChannel.SMS, "01012345678",
                "예약 메시지...", 1, SendRequestStatus.PENDING, future, LocalDateTime.now()
        );
        given(webSendService.listScheduled(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/console/send/scheduled")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].sendId").value("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .andExpect(jsonPath("$.data.content[0].channel").value("SMS"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /console/send/scheduled — 인증 없으면 401")
    void listScheduled_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/console/send/scheduled"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /console/send/scheduled — 빈 목록: 200 + 빈 content")
    void listScheduled_emptyList_returns200EmptyPage() throws Exception {
        given(webSendService.listScheduled(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/console/send/scheduled")
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ── DELETE /console/send/scheduled/{sendId} (예약 취소) ──────────

    @Test
    @DisplayName("DELETE /console/send/scheduled/{sendId} — 취소 성공: 200 + true")
    void cancelScheduled_success_returns200True() throws Exception {
        willDoNothing().given(webSendService)
                .cancelScheduled(eq(1L), eq("01ARZ3NDEKTSV4RRFFQ69G5FAV"), any());

        mockMvc.perform(delete("/console/send/scheduled/01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .with(csrf())
                        .with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WebSendDto.CancelRequest("테스트 취소"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("DELETE /console/send/scheduled/{sendId} — body 없이 취소 가능: 200")
    void cancelScheduled_noBody_returns200() throws Exception {
        willDoNothing().given(webSendService)
                .cancelScheduled(eq(1L), anyString(), any());

        mockMvc.perform(delete("/console/send/scheduled/01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .with(csrf())
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /console/send/scheduled/{sendId} — 없는 sendId: 404")
    void cancelScheduled_unknownSendId_returns404() throws Exception {
        willThrow(new EntityNotFoundException("예약 발송을 찾을 수 없습니다"))
                .given(webSendService)
                .cancelScheduled(eq(1L), eq("NOTEXIST0000000000000000000"), any());

        mockMvc.perform(delete("/console/send/scheduled/NOTEXIST0000000000000000000")
                        .with(csrf())
                        .with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /console/send/scheduled/{sendId} — 인증 없으면 401")
    void cancelScheduled_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/console/send/scheduled/01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /console/send/scheduled/{sendId} — 이미 QUEUED 상태: 400")
    void cancelScheduled_alreadyQueued_returns400() throws Exception {
        willThrow(new IllegalStateException("이미 처리 중인 발송입니다"))
                .given(webSendService)
                .cancelScheduled(eq(1L), eq("01ARZ3NDEKTSV4RRFFQ69G5FAV"), any());

        mockMvc.perform(delete("/console/send/scheduled/01ARZ3NDEKTSV4RRFFQ69G5FAV")
                        .with(csrf())
                        .with(auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
