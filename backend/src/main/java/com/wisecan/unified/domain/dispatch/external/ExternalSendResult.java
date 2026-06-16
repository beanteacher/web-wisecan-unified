package com.wisecan.unified.domain.dispatch.external;

/**
 * 외부 발송 시스템 INSERT 결과 — 합의 스펙 (W-204).
 *
 * <p>외부 시스템이 INSERT 성공 시 반환하는 {@code msg_id} 와
 * INSERT 성공 여부를 담는 응답 VO.</p>
 *
 * <p>실패 시 {@link #success()} 가 {@code false} 이고 {@link #msgId()} 는 {@code null},
 * {@link #errorMessage()} 에 실패 사유가 담긴다.</p>
 *
 * <p>외부 시스템 상태 전이(message_state 1→2→3)와 result_code 는
 * polling 을 통해 별도 조회한다 ({@link ExternalDispatchPollingResult} 참조).</p>
 */
public record ExternalSendResult(

        /**
         * INSERT 성공 여부.
         * {@code true} 이면 외부 시스템이 발송 큐에 등록 완료 (message_state=0).
         */
        boolean success,

        /**
         * 외부 시스템이 발급한 메시지 ID ({@code msg_id}).
         * 성공 시 non-null; 실패 시 null.
         * 이 값으로 이후 polling 조회한다.
         */
        Long msgId,

        /**
         * 실패 사유 — 성공 시 null.
         * 외부 시스템 오류 코드 또는 메시지를 담는다.
         */
        String errorMessage

) {

    /** INSERT 성공 결과를 생성한다. */
    public static ExternalSendResult success(Long msgId) {
        return new ExternalSendResult(true, msgId, null);
    }

    /** INSERT 실패 결과를 생성한다. */
    public static ExternalSendResult failure(String errorMessage) {
        return new ExternalSendResult(false, null, errorMessage);
    }
}
