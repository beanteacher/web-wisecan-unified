package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.dto.dispatch.SendRequestDto;
import com.wisecan.unified.service.dispatch.SendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * wsc.send.single / wsc.send.bulk — 발송 도구 (RQ-MCP-004, 005).
 *
 * <p>비노출 정책 (§7.3): routingMeta 등 내부 라우팅 메타는 응답에 포함하지 않는다.
 * AcceptResponse(sendId, status, recipientCount, totalCost) 만 반환.</p>
 *
 * <p>동등성 보장 (§7.4): SendRequestService 를 직접 호출하여 CLI/SDK 와 동일 결과를 반환한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SendTool {

    private static final long DEFAULT_UNIT_COST = 0L; // 단가는 W-204(과금 게이트) 에서 산출 — 현재 0

    private final SendRequestService sendRequestService;

    // ── 단건 발송 ────────────────────────────────────────────────────────

    @Tool(description = """
            단건 발송을 요청한다 (SMS / LMS / MMS / KAKAO / RCS).
            channel 값: SMS | LMS | MMS | KAKAO | RCS
            카카오 채널은 senderKey, templateCode 필수.
            필수 스코프: send (또는 send:sms | send:kakao | send:rcs)
            응답: sendId, status, recipientCount, totalCost
            (주의: routingMeta 등 내부 정보는 포함되지 않음)
            """)
    public SendRequestDto.AcceptResponse sendSingle(
            @ToolParam(description = "발신번호 (등록된 번호)") String callbackNumber,
            @ToolParam(description = "수신번호") String recipientNumber,
            @ToolParam(description = "발송 채널 (SMS | LMS | MMS | KAKAO | RCS)") String channel,
            @ToolParam(description = "메시지 본문") String messageBody,
            @ToolParam(description = "제목 (LMS/MMS/카카오 선택)") String subject,
            @ToolParam(description = "광고성 여부 (기본 false)") Boolean isAdvertisement,
            @ToolParam(description = "카카오 발신프로필 키 (KAKAO 채널 필수)") String senderKey,
            @ToolParam(description = "카카오 템플릿 코드 (KAKAO 채널 필수)") String templateCode,
            @ToolParam(description = "예약 발송 일시 ISO 8601 (null = 즉시)") String scheduledAt
    ) {
        ApiKeyPrincipal principal = currentPrincipal();
        SendChannel ch = SendChannel.valueOf(channel.toUpperCase().strip());

        SendRequestDto.SingleRequest req = new SendRequestDto.SingleRequest(
                callbackNumber,
                recipientNumber,
                ch,
                subject,
                messageBody,
                Boolean.TRUE.equals(isAdvertisement),
                senderKey,
                templateCode,
                scheduledAt != null ? LocalDateTime.parse(scheduledAt) : null
        );

        return sendRequestService.sendSingle(
                principal.memberId(),
                principal.apiKeyId(),
                DEFAULT_UNIT_COST,
                req
        );
    }

    // ── 다건 발송 ────────────────────────────────────────────────────────

    @Tool(description = """
            다건(일괄) 발송을 요청한다. 최대 수신자 1,000명.
            channel 값: SMS | LMS | MMS | KAKAO | RCS
            필수 스코프: send (또는 send:sms | send:kakao | send:rcs)
            응답: sendId, status, recipientCount, totalCost
            """)
    public SendRequestDto.AcceptResponse sendBulk(
            @ToolParam(description = "발신번호 (등록된 번호)") String callbackNumber,
            @ToolParam(description = "수신번호 목록 (최대 1,000개)") List<String> recipientNumbers,
            @ToolParam(description = "발송 채널 (SMS | LMS | MMS | KAKAO | RCS)") String channel,
            @ToolParam(description = "메시지 본문") String messageBody,
            @ToolParam(description = "제목 (LMS/MMS/카카오 선택)") String subject,
            @ToolParam(description = "광고성 여부 (기본 false)") Boolean isAdvertisement,
            @ToolParam(description = "카카오 발신프로필 키 (KAKAO 채널 필수)") String senderKey,
            @ToolParam(description = "카카오 템플릿 코드 (KAKAO 채널 필수)") String templateCode,
            @ToolParam(description = "예약 발송 일시 ISO 8601 (null = 즉시)") String scheduledAt
    ) {
        ApiKeyPrincipal principal = currentPrincipal();
        SendChannel ch = SendChannel.valueOf(channel.toUpperCase().strip());

        SendRequestDto.BulkRequest req = new SendRequestDto.BulkRequest(
                callbackNumber,
                recipientNumbers,
                ch,
                subject,
                messageBody,
                Boolean.TRUE.equals(isAdvertisement),
                senderKey,
                templateCode,
                scheduledAt != null ? LocalDateTime.parse(scheduledAt) : null
        );

        return sendRequestService.sendBulk(
                principal.memberId(),
                principal.apiKeyId(),
                DEFAULT_UNIT_COST,
                req
        );
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────────

    private ApiKeyPrincipal currentPrincipal() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (auth instanceof ApiKeyPrincipal p) {
            return p;
        }
        throw new IllegalStateException("MCP 발송 도구는 API Key 인증이 필요합니다.");
    }
}
