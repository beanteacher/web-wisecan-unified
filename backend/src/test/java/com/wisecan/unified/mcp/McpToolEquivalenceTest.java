package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.UsageDto;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.dto.dispatch.SendRequestDto;
import com.wisecan.unified.service.UsageService;
import com.wisecan.unified.service.dispatch.SendHistoryService;
import com.wisecan.unified.service.dispatch.SendRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * MCP 동등성 매트릭스 단언 테스트 (W-303 DoD).
 *
 * 검증 항목:
 *   1. GuideTool  - 가이드/스니펫 조회 (동일 코퍼스 반환 단언)
 *   2. SendTool   - 단건/다건 발송 (SendRequestService 위임 단언)
 *   3. HistoryTool - 이력 목록/상세 (SendHistoryService 위임 단언)
 *   4. UsageTool  - 사용량 요약 (UsageService 위임 단언)
 *   5. 비노출 정책 단언 - 보안 민감 액션이 MCP 도구 목록에 없음을 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MCP 동등성 매트릭스 단언 (§7.4)")
class McpToolEquivalenceTest {

    @Mock private SendRequestService sendRequestService;
    @Mock private SendHistoryService sendHistoryService;
    @Mock private UsageService usageService;

    @InjectMocks private SendTool sendTool;
    @InjectMocks private HistoryTool historyTool;
    @InjectMocks private UsageTool usageTool;

    private GuideTool guideTool;
    private SnippetTool snippetTool;

    private static final Long MEMBER_ID  = 1L;
    private static final Long API_KEY_ID = 10L;

    @BeforeEach
    void setUp() {
        guideTool   = new GuideTool();
        snippetTool = new SnippetTool();
        ApiKeyPrincipal principal = new ApiKeyPrincipal(API_KEY_ID, MEMBER_ID,
                Set.of("send", "history:read", "callback:read", "key:read",
                        "balance:read", "template:read", "brand:read"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // 1. GuideTool

    @Test
    @DisplayName("GuideTool - topic null 시 목차 반환")
    void guide_nullTopic_returnsToc() {
        String result = guideTool.guide(null);
        assertThat(result).contains("WiseCan API 가이드 목차");
        assertThat(result).contains("send-sms");
        assertThat(result).contains("웹 콘솔 또는 CLI 전용");
    }

    @Test
    @DisplayName("GuideTool - send-sms 가이드 반환 (동일 코퍼스 단언)")
    void guide_sendSms_returnsGuide() {
        String result = guideTool.guide("send-sms");
        assertThat(result).contains("POST /api/dispatch/send");
        assertThat(result).contains("send:sms");
        assertThat(result).contains("callbackNumber");
    }

    @Test
    @DisplayName("GuideTool - history 가이드에 routingMeta 비노출 명시")
    void guide_history_routingMetaNotExposed() {
        String result = guideTool.guide("history");
        assertThat(result).contains("routingMeta");
        assertThat(result).contains("포함되지 않습니다");
    }

    @Test
    @DisplayName("GuideTool - callback 가이드에 등록/삭제 비노출 명시")
    void guide_callback_registrationNotExposed() {
        String result = guideTool.guide("callback");
        assertThat(result).contains("웹 콘솔 또는 CLI 전용");
    }

    // 2. SnippetTool

    @Test
    @DisplayName("SnippetTool - curl 스니펫 반환 (동일 코퍼스 단언)")
    void snippet_curl_sendSms_returnsSnippet() {
        String result = snippetTool.snippet("curl", "send-sms");
        assertThat(result).contains("X-API-Key");
        assertThat(result).contains("/api/dispatch/send");
        assertThat(result).contains("callbackNumber");
    }

    @Test
    @DisplayName("SnippetTool - python 스니펫 반환 (동일 코퍼스 단언)")
    void snippet_python_sendSms_returnsSnippet() {
        String result = snippetTool.snippet("python", "send-sms");
        assertThat(result).contains("WisecanClient");
        assertThat(result).contains("send_sms");
    }

    @Test
    @DisplayName("SnippetTool - lang null 시 curl 기본값")
    void snippet_nullLang_defaultsCurl() {
        String result = snippetTool.snippet(null, "send-sms");
        assertThat(result).contains("curl");
    }

    // 3. SendTool 동등성

    @Test
    @DisplayName("SendTool.sendSingle - SendRequestService.sendSingle 위임 단언")
    void sendSingle_delegatesToSendRequestService() {
        SendRequestDto.AcceptResponse expected = new SendRequestDto.AcceptResponse(
                "01ARZ3NDEKTSV4RRFFQ69G5FAV", SendRequestStatus.PENDING, 1, 0L);
        given(sendRequestService.sendSingle(anyLong(), anyLong(), anyLong(),
                any(SendRequestDto.SingleRequest.class))).willReturn(expected);

        SendRequestDto.AcceptResponse result = sendTool.sendSingle(
                "01012345678", "01098765432", "SMS",
                "테스트 메시지", null, false, null, null, null);

        assertThat(result.sendId()).isEqualTo(expected.sendId());
        assertThat(result.status()).isEqualTo(SendRequestStatus.PENDING);
        assertThat(result.recipientCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("SendTool.sendBulk - SendRequestService.sendBulk 위임 단언")
    void sendBulk_delegatesToSendRequestService() {
        SendRequestDto.AcceptResponse expected = new SendRequestDto.AcceptResponse(
                "01ARZ3NDEKTSV4RRFFQ69G5FAV", SendRequestStatus.PENDING, 3, 0L);
        given(sendRequestService.sendBulk(anyLong(), anyLong(), anyLong(),
                any(SendRequestDto.BulkRequest.class))).willReturn(expected);

        SendRequestDto.AcceptResponse result = sendTool.sendBulk(
                "01012345678",
                List.of("01011111111", "01022222222", "01033333333"),
                "SMS", "다건 테스트", null, false, null, null, null);

        assertThat(result.recipientCount()).isEqualTo(3);
    }

    // 4. HistoryTool 동등성

    @Test
    @DisplayName("HistoryTool.historyList - SendHistoryService.list 위임 단언")
    void historyList_delegatesToSendHistoryService() {
        SendHistoryDto.PageResponse<SendHistoryDto.ListItem> expected =
                new SendHistoryDto.PageResponse<>(List.of(), 0, 20, 0L, 0);
        given(sendHistoryService.list(anyLong(), anyLong(),
                any(SendHistoryDto.ListParams.class), anyInt(), anyInt()))
                .willReturn(expected);

        SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                historyTool.historyList(null, null, null, null, null, null, 0, 20);

        assertThat(result.totalElements()).isEqualTo(0L);
        assertThat(result.page()).isEqualTo(0);
    }

    @Test
    @DisplayName("HistoryTool.historyDetail - SendHistoryService.detail 위임 단언")
    void historyDetail_delegatesToSendHistoryService() {
        String sendId = "01ARZ3NDEKTSV4RRFFQ69G5FAV";
        SendHistoryDto.DetailItem expected = new SendHistoryDto.DetailItem(
                sendId, SendChannel.SMS, "01012345678", "01098765432",
                1, null, "테스트", SendRequestStatus.QUEUED, null,
                0L, 0L, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        given(sendHistoryService.detail(anyLong(), anyLong(), any())).willReturn(expected);

        SendHistoryDto.DetailItem result = historyTool.historyDetail(sendId);

        assertThat(result.sendId()).isEqualTo(sendId);
        assertThat(result.channel()).isEqualTo(SendChannel.SMS);
    }

    // 5. 비노출 정책 단언

    @Test
    @DisplayName("비노출 정책 - DetailItem 에 routingMeta 필드 없음")
    void historyDetail_doesNotExposeRoutingMeta() {
        var fields = java.util.Arrays.stream(
                SendHistoryDto.DetailItem.class.getRecordComponents())
                .map(rc -> rc.getName())
                .toList();
        assertThat(fields).doesNotContain("routingMeta");
    }

    @Test
    @DisplayName("비노출 정책 - GuideTool 에 키 발급/발신번호 등록/결제 안내 없음")
    void nonExposurePolicy_guideDoesNotExposeSecurityActions() {
        String toc = guideTool.guide(null);
        assertThat(toc).doesNotContain("키 발급");
        assertThat(toc).doesNotContain("발신번호 등록");
        assertThat(toc).doesNotContain("결제");
        assertThat(toc).doesNotContain("충전");
    }

    @Test
    @DisplayName("비노출 정책 - mcp 패키지에 민감 도구 클래스 없음")
    void nonExposurePolicy_noMutativeToolClassExists() {
        String[] forbidden = {
            "com.wisecan.unified.mcp.KeyIssueTool",
            "com.wisecan.unified.mcp.KeyRevokeTool",
            "com.wisecan.unified.mcp.CallbackRegisterTool",
            "com.wisecan.unified.mcp.CallbackDeleteTool",
            "com.wisecan.unified.mcp.PaymentTool",
            "com.wisecan.unified.mcp.ChargeTool",
            "com.wisecan.unified.mcp.RefundTool"
        };
        for (String cls : forbidden) {
            try {
                Class.forName(cls);
                org.junit.jupiter.api.Assertions.fail(
                    "비노출 정책 위반: " + cls + " 이 존재해서는 안 됩니다.");
            } catch (ClassNotFoundException e) {
                // 정상
            }
        }
    }

    // 5b. UsageTool 동등성

    @Test
    @DisplayName("UsageTool.usageSummary - UsageService.getSummary 위임 단언")
    void usageSummary_delegatesToUsageService() {
        UsageDto.SummaryResponse expected = new UsageDto.SummaryResponse(100L, 90L, 10L, 5L);
        given(usageService.getSummary(MEMBER_ID)).willReturn(expected);

        UsageDto.SummaryResponse result = usageTool.usageSummary(null);

        assertThat(result.totalCalls()).isEqualTo(100L);
        assertThat(result.successCount()).isEqualTo(90L);
        assertThat(result.failCount()).isEqualTo(10L);
        assertThat(result.todayCalls()).isEqualTo(5L);
    }
}
