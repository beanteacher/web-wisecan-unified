package com.wisecan.unified.dto.trial;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.trial.TrialDummyContext;
import com.wisecan.unified.domain.trial.TrialSendRecord;
import com.wisecan.unified.domain.trial.TrialSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 체험 모드 DTO 모음 (W-406).
 */
public class TrialDto {

    /** 체험 세션 발급 응답 */
    public record SessionResponse(
            String sessionToken,
            LocalDateTime expiresAt,
            DummyContextSummary dummyContext
    ) {
        public static SessionResponse of(TrialSession session, TrialDummyContext ctx) {
            return new SessionResponse(
                    session.getSessionToken(),
                    session.getExpiresAt(),
                    DummyContextSummary.from(ctx)
            );
        }
    }

    /** 더미 컨텍스트 요약 (FE에 내려주는 표시용 데이터) */
    public record DummyContextSummary(
            String dummyCallbackNumber,
            String dummyApiKey,
            long dummyBalance,
            String dummySendHistoryJson,
            String dummyKakaoTemplateJson,
            String dummyRcsBrandJson
    ) {
        public static DummyContextSummary from(TrialDummyContext ctx) {
            return new DummyContextSummary(
                    ctx.getDummyCallbackNumber(),
                    ctx.getDummyApiKey(),
                    ctx.getDummyBalance(),
                    ctx.getDummySendHistoryJson(),
                    ctx.getDummyKakaoTemplateJson(),
                    ctx.getDummyRcsBrandJson()
            );
        }
    }

    /** 체험 발송 요청 */
    public record SendRequest(
            @NotNull SendChannel channel,
            @NotBlank String recipientNumber,
            @NotBlank String messageBody
    ) {}

    /** 체험 발송 응답 (가상 결과 — 외부 송출 없음 단언 포함) */
    public record SendResponse(
            Long recordId,
            String virtualResultCode,
            boolean externalBlocked,
            String message
    ) {
        public static SendResponse from(TrialSendRecord record) {
            return new SendResponse(
                    record.getId(),
                    record.getVirtualResultCode(),
                    record.isExternalBlocked(),
                    "체험 모드: 실제 발송·결제는 일어나지 않습니다."
            );
        }
    }

    /** 체험 결제 차단 응답 */
    public record BillingBlockedResponse(
            String reason,
            String message
    ) {
        public static BillingBlockedResponse blocked() {
            return new BillingBlockedResponse(
                    "TRIAL_MODE",
                    "체험 모드: 실제 결제·충전은 일어나지 않습니다."
            );
        }
    }
}
