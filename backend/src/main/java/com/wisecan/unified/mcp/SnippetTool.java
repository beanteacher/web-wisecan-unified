package com.wisecan.unified.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * wsc.snippet — 코드 스니펫 조회 (RQ-MCP-003).
 *
 * <p>무인증 도구 — API Key 없이 호출 가능. §7.2 CLI의 {@code wsc snippet} 과 동등.</p>
 */
@Component
public class SnippetTool {

    @Tool(description = """
            WiseCan API 코드 스니펫을 반환한다. 무인증으로 호출 가능하다.
            lang 파라미터: python | java | node | curl (기본 curl)
            action 파라미터: send-sms | send-kakao | send-rcs | history | callback | balance
            """)
    public String snippet(
            @ToolParam(description = "언어 (python | java | node | curl)") String lang,
            @ToolParam(description = "액션 (send-sms | send-kakao | send-rcs | history | callback | balance)") String action
    ) {
        String l = lang == null || lang.isBlank() ? "curl" : lang.toLowerCase().strip();
        String a = action == null || action.isBlank() ? "send-sms" : action.toLowerCase().strip();

        return switch (l) {
            case "python" -> pythonSnippet(a);
            case "java"   -> javaSnippet(a);
            case "node"   -> nodeSnippet(a);
            default       -> curlSnippet(a);
        };
    }

    private String curlSnippet(String action) {
        return switch (action) {
            case "send-sms" -> """
                    # SMS 단건 발송 (curl)
                    curl -X POST https://api.wisecan.kr/api/dispatch/send \\
                      -H "X-API-Key: wc_YOUR_API_KEY" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "callbackNumber": "01012345678",
                        "recipientNumber": "01098765432",
                        "channel": "SMS",
                        "messageBody": "안녕하세요. WiseCan 테스트 메시지입니다."
                      }'
                    """;
            case "send-kakao" -> """
                    # 카카오 알림톡 발송 (curl)
                    curl -X POST https://api.wisecan.kr/api/dispatch/send \\
                      -H "X-API-Key: wc_YOUR_API_KEY" \\
                      -H "Content-Type: application/json" \\
                      -d '{
                        "callbackNumber": "01012345678",
                        "recipientNumber": "01098765432",
                        "channel": "KAKAO",
                        "senderKey": "YOUR_SENDER_KEY",
                        "templateCode": "YOUR_TEMPLATE_CODE",
                        "messageBody": "안녕하세요. #{name}님."
                      }'
                    """;
            case "history" -> """
                    # 발송 이력 목록 조회 (curl)
                    curl -X GET "https://api.wisecan.kr/api/dispatch/history?page=0&size=20" \\
                      -H "X-API-Key: wc_YOUR_API_KEY"
                    """;
            case "balance" -> """
                    # 잔액 조회 (curl)
                    curl -X GET https://api.wisecan.kr/api/balance \\
                      -H "X-API-Key: wc_YOUR_API_KEY"
                    """;
            default -> "# curl 스니펫 — action: " + action + " 은 지원하지 않습니다.";
        };
    }

    private String pythonSnippet(String action) {
        return switch (action) {
            case "send-sms" -> """
                    # SMS 단건 발송 (Python)
                    from wisecan import WisecanClient

                    client = WisecanClient(api_key="wc_YOUR_API_KEY")
                    result = client.send_sms(
                        callback_number="01012345678",
                        recipient_number="01098765432",
                        message_body="안녕하세요. WiseCan 테스트 메시지입니다."
                    )
                    print(result.send_id, result.status)
                    """;
            case "history" -> """
                    # 발송 이력 조회 (Python)
                    from wisecan import WisecanClient

                    client = WisecanClient(api_key="wc_YOUR_API_KEY")
                    history = client.history.list(page=0, size=20)
                    for item in history.content:
                        print(item.send_id, item.status, item.total_cost)
                    """;
            default -> "# Python 스니펫 — action: " + action + " 은 지원하지 않습니다.";
        };
    }

    private String javaSnippet(String action) {
        return switch (action) {
            case "send-sms" -> """
                    // SMS 단건 발송 (Java)
                    WisecanClient client = new WisecanClient("wc_YOUR_API_KEY");
                    SendResult result = client.sendSms(SendSmsRequest.builder()
                        .callbackNumber("01012345678")
                        .recipientNumber("01098765432")
                        .messageBody("안녕하세요. WiseCan 테스트 메시지입니다.")
                        .build());
                    System.out.println(result.getSendId() + " " + result.getStatus());
                    """;
            default -> "// Java 스니펫 — action: " + action + " 은 지원하지 않습니다.";
        };
    }

    private String nodeSnippet(String action) {
        return switch (action) {
            case "send-sms" -> """
                    // SMS 단건 발송 (Node.js)
                    const { WisecanClient } = require('@wisecan/sdk');

                    const client = new WisecanClient({ apiKey: 'wc_YOUR_API_KEY' });
                    const result = await client.sendSms({
                      callbackNumber: '01012345678',
                      recipientNumber: '01098765432',
                      messageBody: '안녕하세요. WiseCan 테스트 메시지입니다.'
                    });
                    console.log(result.sendId, result.status);
                    """;
            default -> "// Node.js 스니펫 — action: " + action + " 은 지원하지 않습니다.";
        };
    }
}
