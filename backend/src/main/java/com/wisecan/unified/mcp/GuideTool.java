package com.wisecan.unified.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * wsc.guide — API 가이드·문서 조회 (RQ-MCP-002).
 *
 * <p>비노출 정책 (§7.3): 키 발급/폐기, 발신번호 등록/삭제, 결제/충전 관련 내용은
 * 가이드 본문에 포함되지 않는다. 조회 전용 read-only 도구.</p>
 */
@Component
public class GuideTool {

    @Tool(description = """
            WiseCan 통합 메시징 API 가이드를 조회한다.
            topic 파라미터로 원하는 주제를 지정하면 해당 가이드 문서를 반환한다.
            topic 예시: send-sms, send-kakao, send-rcs, history, callback, api-key, balance, template, brand
            topic 을 지정하지 않으면 전체 목차를 반환한다.
            (주의: 키 발급/폐기·발신번호 등록/삭제·결제 액션은 웹 콘솔 또는 CLI 에서만 수행 가능)
            """)
    public String guide(@ToolParam(description = "조회할 가이드 주제 (null 이면 목차 반환)") String topic) {
        if (topic == null || topic.isBlank()) {
            return """
                    # WiseCan API 가이드 목차

                    - send-sms       : SMS / LMS / MMS 발송 가이드
                    - send-kakao     : 카카오 알림톡 발송 가이드
                    - send-rcs       : RCS 발송 가이드
                    - history        : 발송 이력 조회 가이드
                    - callback       : 발신번호 조회 가이드
                    - api-key        : API 키 조회 가이드
                    - balance        : 잔액 조회 가이드
                    - template       : 카카오·RCS 템플릿 조회 가이드
                    - brand          : RCS 브랜드 조회 가이드

                    ※ 키 발급/폐기·발신번호 등록/삭제·결제는 웹 콘솔 또는 CLI 전용입니다.
                    """;
        }
        return switch (topic.toLowerCase().strip()) {
            case "send-sms" -> """
                    # SMS / LMS / MMS 발송

                    엔드포인트: POST /api/dispatch/send (단건) | POST /api/dispatch/send/bulk (다건)
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: send 또는 send:sms

                    필수 필드:
                      - callbackNumber  : 등록된 발신번호 (예: 01012345678)
                      - recipientNumber : 수신번호 (단건) | recipientNumbers: [수신번호...] (다건)
                      - channel         : SMS | LMS | MMS
                      - messageBody     : 메시지 본문

                    선택 필드:
                      - subject         : 제목 (LMS/MMS)
                      - isAdvertisement : 광고성 여부 (기본 false)
                      - scheduledAt     : 예약 발송 일시 (ISO 8601)

                    응답: { sendId, status, recipientCount, totalCost }
                    """;
            case "send-kakao" -> """
                    # 카카오 알림톡 발송

                    엔드포인트: POST /api/dispatch/send
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: send 또는 send:kakao

                    필수 필드:
                      - callbackNumber : 등록된 발신번호
                      - recipientNumber: 수신번호
                      - channel        : KAKAO
                      - senderKey      : 카카오 발신프로필 키
                      - templateCode   : 카카오 템플릿 코드
                      - messageBody    : 메시지 본문

                    응답: { sendId, status, recipientCount, totalCost }
                    """;
            case "send-rcs" -> """
                    # RCS 발송

                    엔드포인트: POST /api/dispatch/send
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: send 또는 send:rcs

                    필수 필드:
                      - callbackNumber : 등록된 발신번호
                      - recipientNumber: 수신번호
                      - channel        : RCS
                      - messageBody    : 메시지 본문

                    응답: { sendId, status, recipientCount, totalCost }
                    """;
            case "history" -> """
                    # 발송 이력 조회

                    목록: GET /api/dispatch/history?fromDate=&toDate=&channel=&page=0&size=20
                    상세: GET /api/dispatch/history/{sendId}
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: history:read

                    응답 (목록 항목): { sendId, channel, callbackNumber, recipientCount, status, totalCost, requestedAt }
                    응답 (상세): 위 필드 + subject, messageBody, failReason, unitCost, externalMsgId
                    ※ routingMeta(내부 라우팅 정보)는 응답에 포함되지 않습니다.
                    """;
            case "callback" -> """
                    # 발신번호 조회

                    목록: GET /api/callback/sender-numbers
                    상세: GET /api/callback/sender-numbers/{id}
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: callback:read

                    응답: { id, phoneNumber, status, registerType, description, createdAt }
                    ※ 발신번호 등록/삭제는 웹 콘솔 또는 CLI 전용입니다.
                    """;
            case "api-key" -> """
                    # API 키 조회

                    목록: GET /api/keys
                    필수 헤더: Authorization: Bearer <session-token>
                    필수 스코프: key:read

                    응답: [{ id, keyName, keyPrefix, status, keyType, scopes, dailyLimit, createdAt }]
                    ※ 키 발급/폐기/재발급은 웹 콘솔 또는 CLI 전용입니다.
                    """;
            case "balance" -> """
                    # 잔액 조회

                    엔드포인트: GET /api/balance
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: balance:read

                    응답: { balance, currency, lastChargedAt }
                    ※ 충전/환불은 웹 콘솔 전용입니다.
                    """;
            case "template" -> """
                    # 카카오·RCS 템플릿 조회

                    카카오: GET /api/templates/kakao?page=0&size=20
                    RCS   : GET /api/templates/rcs?page=0&size=20
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: template:read

                    응답 (카카오): { templateCode, templateName, status, content, buttons }
                    응답 (RCS)   : { templateId, templateName, status, messageType }
                    """;
            case "brand" -> """
                    # RCS 브랜드 조회

                    엔드포인트: GET /api/brands/rcs?page=0&size=20
                    필수 헤더: X-API-Key: <your-api-key>
                    필수 스코프: brand:read

                    응답: { brandId, brandName, status, logoUrl, description }
                    """;
            default -> "알 수 없는 가이드 주제입니다: " + topic
                    + "\n사용 가능한 주제: send-sms, send-kakao, send-rcs, history, callback, api-key, balance, template, brand";
        };
    }
}
