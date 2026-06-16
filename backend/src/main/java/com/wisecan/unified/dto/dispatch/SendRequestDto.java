package com.wisecan.unified.dto.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 발송 요청 적재 DTO.
 *
 * <p>backend-conventions.md 규칙 2에 따라 도메인별 DTO 파일 하나에 inner record로 선언.</p>
 */
public class SendRequestDto {

    // ── 요청 ─────────────────────────────────────────────────────────

    /**
     * 단건 발송 요청.
     * API Key 인증 헤더와 함께 {@code POST /dispatch/send} 로 전달된다.
     */
    public record SingleRequest(

            @NotBlank(message = "발신번호는 필수입니다")
            @Size(max = 20, message = "발신번호는 최대 20자입니다")
            String callbackNumber,

            @NotBlank(message = "수신번호는 필수입니다")
            @Size(max = 20, message = "수신번호는 최대 20자입니다")
            String recipientNumber,

            @NotNull(message = "발송 채널은 필수입니다")
            SendChannel channel,

            /** 메시지 제목 (LMS/MMS/카카오에서 사용, SMS는 null 허용) */
            @Size(max = 120, message = "제목은 최대 120자입니다")
            String subject,

            @NotBlank(message = "메시지 본문은 필수입니다")
            String messageBody,

            /** 광고성 메시지 여부 — 미지정 시 false */
            boolean isAdvertisement,

            /** 카카오 발신프로필 키 (KAKAO 채널 필수) */
            @Size(max = 40)
            String senderKey,

            /** 카카오 템플릿 코드 (KAKAO 채널 필수) */
            @Size(max = 50)
            String templateCode,

            /**
             * 예약 발송 일시 — null이면 즉시 발송.
             * (05_DATA_MODEL.md §5.5 request_date)
             */
            LocalDateTime scheduledAt

    ) {}

    /**
     * 다건(일괄) 발송 요청.
     * 동일 {@code group_id}로 묶여 외부 시스템에 적재된다.
     */
    public record BulkRequest(

            @NotBlank(message = "발신번호는 필수입니다")
            @Size(max = 20)
            String callbackNumber,

            @NotNull(message = "수신자 목록은 필수입니다")
            @Size(min = 1, max = 1000, message = "수신자는 1명 이상 1,000명 이하입니다")
            List<@NotBlank @Size(max = 20) String> recipientNumbers,

            @NotNull(message = "발송 채널은 필수입니다")
            SendChannel channel,

            @Size(max = 120)
            String subject,

            @NotBlank(message = "메시지 본문은 필수입니다")
            String messageBody,

            boolean isAdvertisement,

            @Size(max = 40)
            String senderKey,

            @Size(max = 50)
            String templateCode,

            LocalDateTime scheduledAt

    ) {}

    // ── 응답 ─────────────────────────────────────────────────────────

    /**
     * 발송 적재 응답 — send_id(ULID)만 외부에 노출.
     * (05_DATA_MODEL.md §14 케이스①: 응답 send_id (ULID))
     */
    public record AcceptResponse(
            /** 발송 요청 단일 식별자 (ULID 26자) */
            String sendId,
            SendRequestStatus status,
            int recipientCount,
            long totalCost
    ) {
        public static AcceptResponse from(SendRequest entity) {
            return new AcceptResponse(
                    entity.getSendId(),
                    entity.getStatus(),
                    entity.getRecipientCount(),
                    entity.getTotalCost()
            );
        }
    }

    /**
     * 발송 요청 상세 조회 응답.
     * 라우팅 메타(routingMeta)는 포함하지 않는다 — 회원 UI 비노출 (INV-02).
     */
    public record DetailResponse(
            String sendId,
            Long memberId,
            SendChannel channel,
            SmsMessageType smsType,
            String callbackNumber,
            int recipientCount,
            String subject,
            SendRequestStatus status,
            Long externalMsgId,
            long unitCost,
            long totalCost,
            LocalDateTime requestedAt,
            LocalDateTime createdAt
    ) {
        public static DetailResponse from(SendRequest entity) {
            return new DetailResponse(
                    entity.getSendId(),
                    entity.getMemberId(),
                    entity.getChannel(),
                    entity.getSmsType(),
                    entity.getCallbackNumber(),
                    entity.getRecipientCount(),
                    entity.getSubject(),
                    entity.getStatus(),
                    entity.getExternalMsgId(),
                    entity.getUnitCost(),
                    entity.getTotalCost(),
                    entity.getRequestedAt(),
                    entity.getCreatedAt()
            );
        }
    }
}
