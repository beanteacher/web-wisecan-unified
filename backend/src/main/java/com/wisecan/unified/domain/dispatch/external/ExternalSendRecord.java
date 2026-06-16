package com.wisecan.unified.domain.dispatch.external;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;

import java.time.LocalDateTime;

/**
 * 외부 발송 시스템 INSERT 페이로드 — 합의 스펙 (W-204).
 *
 * <p>본 서비스가 외부 발송 시스템의 {@code send_*_tran} 테이블에 INSERT할 때
 * 채워야 하는 컬럼 집합을 표현한다. 05_DATA_MODEL.md §5.1·§5.2 기준.</p>
 *
 * <p><b>합의 규약:</b></p>
 * <ul>
 *   <li>{@code msgType} / {@code msgSubType} — 채널·인코딩 분기 결과 (§5.2)</li>
 *   <li>{@code messageState = 0} — 본 서비스가 INSERT 직후 초기값 (§5.3 init)</li>
 *   <li>{@code requestDate} — 즉시 발송: 현재 시각 / 예약: 회원 지정 일시</li>
 *   <li>{@code userId} — 내부 memberId 를 외부 시스템 user_id 에 매핑</li>
 *   <li>{@code groupId} — 일괄 발송 묶음 식별; 단건은 NULL</li>
 *   <li>{@code etcChar1} — 내부 send_id(ULID) 보관 — 역추적용 (회원 비노출)</li>
 *   <li>{@code etcChar2} — 라우팅 메타(중계사 정보) — 회원·API 응답에 절대 비노출 (INV-02)</li>
 *   <li>{@code fbMsg} / {@code fbFileCount} — 카카오·RCS 폴백 SMS/LMS 내용 (§5.5 대체발송)</li>
 * </ul>
 *
 * <p>송출·상태 전이·{@code result_*} 컬럼 채움은 모두 외부 시스템 책임 (§5.4).</p>
 */
public record ExternalSendRecord(

        // ── 메시지 유형 ───────────────────────────────────────────────
        /** 메시지 유형 코드 (예: SMS, LMS, MMS, AT, RCS) */
        String msgType,

        /** 메시지 세부 유형 (예: NORM, AD / AT, FT, EX / TEXT, TMPL) */
        String msgSubType,

        // ── 수신·발신 ─────────────────────────────────────────────────
        /** 발신번호 (정규화된 E.164 또는 국내 형식) */
        String callback,

        /** 수신자 번호 목록 — 단건은 1개, 다건은 쉼표 구분 문자열 */
        String recipientNumbers,

        /** 수신자 수 */
        int recipientCount,

        // ── 메시지 본문 ───────────────────────────────────────────────
        /** 메시지 제목 (LMS·MMS·카카오 등) */
        String subject,

        /** 메시지 본문 */
        String messageBody,

        // ── 카카오·RCS 전용 ───────────────────────────────────────────
        /** 카카오 발신프로필 키 (KAKAO 채널만) */
        String senderKey,

        /** 카카오 템플릿 코드 (KAKAO 채널만) */
        String templateCode,

        // ── 폴백 (대체발송) ───────────────────────────────────────────
        /** 폴백 SMS/LMS 본문 (카카오·RCS 1차 실패 시 외부 시스템이 자동 재발송) */
        String fbMsg,

        /** 폴백 첨부파일 개수 (기본 0) */
        int fbFileCount,

        // ── 예약·그룹 ─────────────────────────────────────────────────
        /** 전송 희망 일시 (즉시=현재 시각, 예약=미래 시각) */
        LocalDateTime requestDate,

        /** 일괄 발송 그룹 ID (단건은 NULL) */
        Long groupId,

        // ── 비용 ──────────────────────────────────────────────────────
        /** 건당 발송 단가 (원화) */
        long unitCost,

        // ── 식별자·상태 ───────────────────────────────────────────────
        /** 내부 회원 ID (외부 시스템 user_id 컬럼 매핑) */
        Long userId,

        /**
         * 메시지 상태 — INSERT 시점에는 항상 0(init).
         * 이후 전이(1·2·3·4)는 외부 시스템 책임 (§5.3).
         */
        int messageState,

        // ── 예비 컬럼 (etc_char_*) ────────────────────────────────────
        /**
         * 내부 send_id (ULID 26자) — 역추적용 보관.
         * 외부 msg_id ↔ 내부 sendId 매핑에 사용한다.
         * 회원·API 응답에 직접 노출하지 않는다.
         */
        String etcChar1,

        /**
         * 라우팅 메타 — 중계사 매핑 정보.
         * 회원 UI·API 응답·CLI·MCP 어디에도 절대 노출 금지 (INV-02, 05_DATA_MODEL §5.5).
         */
        String etcChar2

) {

    /**
     * {@link SendRequest} 엔티티로부터 외부 INSERT 페이로드를 생성한다.
     *
     * @param req          내부 발송 요청 엔티티
     * @param routingMeta  중계사 라우팅 메타 (etc_char_2 에 저장, 회원 비노출)
     * @param fbMsg        폴백 본문 (카카오·RCS 채널만 의미 있음, 그 외 null)
     * @return 외부 INSERT 페이로드
     */
    public static ExternalSendRecord from(SendRequest req, String routingMeta, String fbMsg) {
        String msgType    = resolveMsgType(req);
        String msgSubType = resolveMsgSubType(req);

        return new ExternalSendRecord(
                msgType,
                msgSubType,
                req.getCallbackNumber(),
                req.getRecipientNumbers(),
                req.getRecipientCount(),
                req.getSubject(),
                req.getMessageBody(),
                req.getSenderKey(),
                req.getTemplateCode(),
                fbMsg,
                0,
                req.getRequestedAt(),
                req.getGroupId(),
                req.getUnitCost(),
                req.getMemberId(),
                0,              // messageState = 0 (init) — §5.3
                req.getSendId(), // etcChar1 = 내부 sendId — 역추적용
                routingMeta      // etcChar2 = 라우팅 메타 — 회원 비노출
        );
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────

    /**
     * 채널·인코딩 결과로부터 외부 시스템 {@code msg_type} 을 산출한다.
     *
     * <p>외부 시스템 합의 코드 (05_DATA_MODEL §5.2 msg_type 산출 근거):
     * <ul>
     *   <li>SMS → "SMS"</li>
     *   <li>LMS → "LMS"</li>
     *   <li>MMS → "MMS"</li>
     *   <li>KAKAO → "AT" (알림톡)</li>
     *   <li>RCS → "RCS"</li>
     * </ul>
     */
    private static String resolveMsgType(SendRequest req) {
        return switch (req.getChannel()) {
            case SMS   -> "SMS";
            case LMS   -> "LMS";
            case MMS   -> "MMS";
            case KAKAO -> "AT";
            case RCS   -> "RCS";
        };
    }

    /**
     * 채널·광고 여부로부터 외부 시스템 {@code msg_sub_type} 을 산출한다.
     *
     * <p>합의 코드:
     * <ul>
     *   <li>SMS/LMS/MMS 일반 → "NORM"</li>
     *   <li>SMS/LMS/MMS 광고 → "AD"</li>
     *   <li>KAKAO 알림톡 → "AT"</li>
     *   <li>RCS 템플릿 발송 → "TMPL" / 일반 텍스트 → "TEXT"</li>
     * </ul>
     */
    private static String resolveMsgSubType(SendRequest req) {
        return switch (req.getChannel()) {
            case SMS, LMS, MMS -> req.isAdvertisementMessage() ? "AD" : "NORM";
            case KAKAO         -> "AT";
            case RCS           -> req.getTemplateCode() != null ? "TMPL" : "TEXT";
        };
    }
}
