package com.wisecan.unified.dto.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 웹 콘솔 발송 DTO (W-206).
 *
 * <p>JWT 인증 기반 회원 세션에서 호출되는 발송 엔드포인트 전용 DTO.
 * API Key 기반 {@link SendRequestDto}와 분리하여 웹 콘솔 고유 필드를 관리한다.</p>
 *
 * <p>02_FEATURE_SPEC.md §6.1(단건), §6.2(일괄), §6.3(예약) 참조.</p>
 */
public class WebSendDto {

    // ── 단건 발송 요청 ─────────────────────────────────────────────────

    /**
     * 단건 발송 요청 — POST /console/send.
     *
     * <p>수신번호는 최대 1,000개까지 직접 입력 가능하다 (와이어프레임 08.messages-channel.html).
     * 대량 발송은 일괄 발송(§6.2)을 사용한다.</p>
     */
    public record SingleRequest(

            @NotBlank(message = "발신번호는 필수입니다")
            @Size(max = 20, message = "발신번호는 최대 20자입니다")
            String callbackNumber,

            @NotEmpty(message = "수신번호는 최소 1개 이상이어야 합니다")
            @Size(max = 1000, message = "수신번호는 최대 1,000개입니다")
            List<@NotBlank @Size(max = 20) String> recipientNumbers,

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
            String templateCode

    ) {}

    // ── 일괄 발송 요청 ─────────────────────────────────────────────────

    /**
     * 일괄 발송 요청 — POST /console/send/bulk.
     *
     * <p>CSV 파싱 결과를 받아 ≤ 100,000행/요청을 처리한다 (02 §6.2).
     * 변수 치환은 클라이언트에서 처리 후 서버에 전달하거나,
     * messageBody에 #{변수} 패턴을 포함해 전달한다.</p>
     */
    public record BulkRequest(

            @NotBlank(message = "발신번호는 필수입니다")
            @Size(max = 20)
            String callbackNumber,

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

            /**
             * 수신자 번호 목록 — CSV 파싱 결과.
             * 최대 100,000행 (02 §6.2 사전조건).
             */
            @NotEmpty(message = "수신자 목록은 필수입니다")
            @Size(max = 100000, message = "수신자는 최대 100,000명입니다")
            List<@NotBlank @Size(max = 20) String> recipientNumbers

    ) {}

    // ── 예약 발송 요청 ─────────────────────────────────────────────────

    /**
     * 예약 발송 요청 — POST /console/send/scheduled.
     *
     * <p>발송 시각 이전이면 취소 가능하다 (02 §6.3).
     * 적재 → 실제 송출 시각 편차 ≤ 60초 NFR.</p>
     */
    public record ScheduledRequest(

            @NotBlank(message = "발신번호는 필수입니다")
            @Size(max = 20)
            String callbackNumber,

            @NotEmpty(message = "수신번호는 최소 1개 이상이어야 합니다")
            @Size(max = 1000)
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

            /**
             * 예약 발송 일시 — 반드시 미래 시각.
             * (05_DATA_MODEL.md §5.5 request_date)
             */
            @NotNull(message = "예약 일시는 필수입니다")
            @Future(message = "예약 일시는 미래 시각이어야 합니다")
            LocalDateTime scheduledAt

    ) {}

    // ── 예약 취소 요청 ─────────────────────────────────────────────────

    /**
     * 예약 발송 취소 요청 — DELETE /console/send/scheduled/{sendId}.
     */
    public record CancelRequest(
            /** 취소 사유 (선택) */
            @Size(max = 500)
            String reason
    ) {}

    // ── 공통 응답 ──────────────────────────────────────────────────────

    /**
     * 발송 적재 응답 — send_id(ULID)만 외부에 노출.
     */
    public record AcceptResponse(
            String sendId,
            SendRequestStatus status,
            int recipientCount,
            long totalCost,
            /** 예약 발송의 경우 예약 일시 포함 */
            LocalDateTime scheduledAt
    ) {
        public static AcceptResponse from(SendRequest entity) {
            return new AcceptResponse(
                    entity.getSendId(),
                    entity.getStatus(),
                    entity.getRecipientCount(),
                    entity.getTotalCost(),
                    entity.getRequestedAt()
            );
        }
    }

    /**
     * 부분 발송 응답 — HTTP 207 Multi-Status (W-405).
     *
     * 02_FEATURE_SPEC §11.1: 대량 발송 중 잔액 부족 시 일부만 적재 완료된 경우.
     */
    public record PartialSendResponse(
            String acceptedSendId,
            int acceptedCount,
            int rejectedCount,
            List<String> rejectedNumbers,
            String rejectReason,
            long shortfall
    ) {
        public static PartialSendResponse of(
                String acceptedSendId,
                int acceptedCount,
                List<String> rejectedNumbers,
                long shortfall
        ) {
            return new PartialSendResponse(
                    acceptedSendId,
                    acceptedCount,
                    rejectedNumbers.size(),
                    List.copyOf(rejectedNumbers),
                    "INSUFFICIENT_BALANCE",
                    shortfall
            );
        }
    }

    /**
     * 예약 발송 목록 응답.
     */
    public record ScheduledSummary(
            String sendId,
            SendChannel channel,
            String callbackNumber,
            String messagePreview,
            int recipientCount,
            SendRequestStatus status,
            LocalDateTime scheduledAt,
            LocalDateTime createdAt
    ) {
        public static ScheduledSummary from(SendRequest entity) {
            String body = entity.getMessageBody();
            String preview = body.length() > 50 ? body.substring(0, 50) + "..." : body;
            return new ScheduledSummary(
                    entity.getSendId(),
                    entity.getChannel(),
                    entity.getCallbackNumber(),
                    preview,
                    entity.getRecipientCount(),
                    entity.getStatus(),
                    entity.getRequestedAt(),
                    entity.getCreatedAt()
            );
        }
    }
}
