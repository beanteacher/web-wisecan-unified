package com.wisecan.unified.dto.sendernumber;

import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CallbackDto {

    // ── 요청 ──────────────────────────────────────────────────────

    /**
     * 발신번호 등록 요청 (4 케이스 공통).
     * SELF_MOBILE / SELF_LANDLINE 은 서류 없이 이 요청만으로 처리.
     * EMPLOYEE / CORP_REP 는 별도 서류 업로드 엔드포인트를 병행 사용.
     */
    public record RegisterRequest(
        @NotBlank @Size(max = 20)
        String phoneNumber,

        @NotNull
        CallbackRegisterType registerType,

        @Size(max = 100)
        String description
    ) {}

    /** 운영자 심사 승인 요청 */
    public record ApproveRequest(
        String comment
    ) {}

    /** 운영자 심사 반려 요청 */
    public record RejectRequest(
        @NotBlank @Size(max = 500)
        String reason
    ) {}

    // ── 응답 ──────────────────────────────────────────────────────

    /** 발신번호 단건 응답 */
    public record Response(
        Long id,
        String phoneNumber,
        CallbackRegisterType registerType,
        String description,
        CallbackStatus status,
        String rejectReason,
        LocalDateTime registeredAt,
        LocalDateTime createdAt
    ) {
        public static Response from(Callback callback) {
            return new Response(
                callback.getId(),
                callback.getPhoneNumber(),
                callback.getRegisterType(),
                callback.getDescription(),
                callback.getStatus(),
                callback.getRejectReason(),
                callback.getRegisteredAt(),
                callback.getCreatedAt()
            );
        }
    }

    /** 발신번호 목록 응답 (Summary) */
    public record Summary(
        Long id,
        String phoneNumber,
        CallbackRegisterType registerType,
        String description,
        CallbackStatus status,
        LocalDateTime registeredAt
    ) {
        public static Summary from(Callback callback) {
            return new Summary(
                callback.getId(),
                callback.getPhoneNumber(),
                callback.getRegisterType(),
                callback.getDescription(),
                callback.getStatus(),
                callback.getRegisteredAt()
            );
        }
    }

    /** 등록 결과 응답 */
    public record RegisterResponse(
        Long id,
        String phoneNumber,
        CallbackStatus status,
        String message
    ) {}
}
