package com.wisecan.unified.domain.dispatch;

import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발송 요청 적재 엔티티.
 *
 * <p>검증(W-201)·인코딩(W-202) 통과 후 외부 발송 시스템에 INSERT하기 전
 * 본 서비스 내부에 기록하는 발송 메타·라우팅 정보의 단일 소유 테이블이다.</p>
 *
 * <p>식별자는 ULID({@link UlidGenerator#generate()}) 단일 사용.
 * DB PK는 성능(인덱스 순차 삽입)을 위해 BIGINT AUTO_INCREMENT를 유지하고,
 * 외부 노출·API 응답에는 {@code sendId}(ULID 26자)만 사용한다.</p>
 *
 * <p>05_DATA_MODEL.md §5 / 02_FEATURE_SPEC.md §6.1 참조.</p>
 */
@Entity
@Table(
    name = "send_request",
    indexes = {
        @Index(name = "idx_send_request_send_id",    columnList = "send_id",    unique = true),
        @Index(name = "idx_send_request_member_id",  columnList = "member_id"),
        @Index(name = "idx_send_request_api_key_id", columnList = "api_key_id"),
        @Index(name = "idx_send_request_status",     columnList = "status, created_at")
    }
)
@Getter
@NoArgsConstructor
public class SendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 식별자 ────────────────────────────────────────────────────────

    /**
     * 발송 요청 단일 식별자 — ULID 26자.
     * API 응답·외부 시스템 연계 시 이 값만 노출한다.
     * (05_DATA_MODEL.md §표준 자릿수: ULID = 26)
     */
    @Column(name = "send_id", nullable = false, length = 26, updatable = false)
    private String sendId;

    // ── 요청자 메타 ───────────────────────────────────────────────────

    /** 발송 요청 회원 ID (MEMBER.id 참조, JOIN 없이 FK 값만 보관) */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 인증에 사용된 API Key ID (API_KEY.id 참조) */
    @Column(name = "api_key_id", nullable = false)
    private Long apiKeyId;

    // ── 채널·타입 ─────────────────────────────────────────────────────

    /**
     * 발송 채널 — SMS/LMS/MMS/KAKAO/RCS.
     * (05_DATA_MODEL.md §5.2 msg_type 산출 근거)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SendChannel channel;

    /**
     * SMS 계열 메시지 타입 — 인코딩 분기 결과.
     * 카카오·RCS 채널일 때는 NULL.
     * (W-202 SmsEncoding.resolveType() 결과)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sms_type", length = 5)
    private SmsMessageType smsType;

    // ── 수신·발신 ─────────────────────────────────────────────────────

    /** 발신번호 (정규화된 번호, E.164 형식) */
    @Column(name = "callback_number", nullable = false, length = 20)
    private String callbackNumber;

    /**
     * 수신자 번호 목록 — 쉼표 구분 문자열.
     * 단건은 번호 1개, 다건은 N개 쉼표 구분.
     * (05_DATA_MODEL.md §5.5 단건=다건=예약)
     */
    @Column(name = "recipient_numbers", nullable = false, columnDefinition = "TEXT")
    private String recipientNumbers;

    /** 수신자 수 (잔액 차감·통계 기준) */
    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    // ── 메시지 본문 ───────────────────────────────────────────────────

    /** 메시지 제목 (LMS/MMS/카카오 등에서 사용, SMS는 NULL) */
    @Column(name = "subject", length = 120)
    private String subject;

    /** 메시지 본문 (원문 UTF-8) */
    @Column(name = "message_body", nullable = false, columnDefinition = "TEXT")
    private String messageBody;

    /** 광고성 메시지 여부 (Y/N) */
    @Column(name = "is_advertisement", nullable = false, length = 1)
    private String isAdvertisement;

    // ── 카카오 전용 ───────────────────────────────────────────────────

    /** 카카오 발신프로필 키 (KAKAO 채널만 사용, 그 외 NULL) */
    @Column(name = "sender_key", length = 40)
    private String senderKey;

    /** 카카오 템플릿 코드 (KAKAO 채널만 사용, 그 외 NULL) */
    @Column(name = "template_code", length = 50)
    private String templateCode;

    // ── 예약·그룹 ─────────────────────────────────────────────────────

    /**
     * 전송 희망 일시.
     * 즉시 발송은 현재 시각, 예약 발송은 미래 시각.
     * (05_DATA_MODEL.md §5.2 request_date)
     */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /**
     * 일괄 발송 그룹 ID (외부 시스템 group_id 매핑용).
     * 단건 발송은 NULL.
     * (05_DATA_MODEL.md §5.5 단건=다건=예약)
     */
    @Column(name = "group_id")
    private Long groupId;

    // ── 라우팅 메타 ───────────────────────────────────────────────────

    /**
     * 중계사 라우팅 메타 — JSON 문자열.
     * 외부 시스템 INSERT 시 etc_char_* 컬럼에 매핑될 라우팅 정보.
     * 회원 UI·API 응답에 노출하지 않는다 (INV-02, 01_PRD 차별화②).
     */
    @Column(name = "routing_meta", columnDefinition = "TEXT")
    private String routingMeta;

    /**
     * 외부 발송 시스템이 발급한 메시지 ID (msg_id).
     * 외부 INSERT 성공 후 업데이트된다.
     */
    @Column(name = "external_msg_id")
    private Long externalMsgId;

    // ── 비용 ──────────────────────────────────────────────────────────

    /** 건당 발송 단가 (원화) */
    @Column(name = "unit_cost", nullable = false)
    private long unitCost;

    /** 총 차감 금액 = recipientCount × unitCost */
    @Column(name = "total_cost", nullable = false)
    private long totalCost;

    // ── 상태 ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SendRequestStatus status;

    /** 실패 사유 (FAILED/CANCELLED 시 기록) */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    // ── 감사 ──────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── 팩토리 메서드 ─────────────────────────────────────────────────

    /**
     * 발송 요청 적재 엔티티를 생성한다.
     *
     * <p>검증(W-201)·인코딩(W-202) 통과 후 호출한다.
     * sendId는 내부에서 ULID를 자동 생성한다.</p>
     */
    @Builder
    public SendRequest(
            Long memberId,
            Long apiKeyId,
            SendChannel channel,
            SmsMessageType smsType,
            String callbackNumber,
            String recipientNumbers,
            int recipientCount,
            String subject,
            String messageBody,
            boolean isAdvertisement,
            String senderKey,
            String templateCode,
            LocalDateTime requestedAt,
            Long groupId,
            String routingMeta,
            long unitCost
    ) {
        this.sendId           = UlidGenerator.generate();
        this.memberId         = memberId;
        this.apiKeyId         = apiKeyId;
        this.channel          = channel;
        this.smsType          = smsType;
        this.callbackNumber   = callbackNumber;
        this.recipientNumbers = recipientNumbers;
        this.recipientCount   = recipientCount;
        this.subject          = subject;
        this.messageBody      = messageBody;
        this.isAdvertisement  = isAdvertisement ? "Y" : "N";
        this.senderKey        = senderKey;
        this.templateCode     = templateCode;
        this.requestedAt      = requestedAt != null ? requestedAt : LocalDateTime.now();
        this.groupId          = groupId;
        this.routingMeta      = routingMeta;
        this.unitCost         = unitCost;
        this.totalCost        = (long) recipientCount * unitCost;
        this.status           = SendRequestStatus.PENDING;
    }

    // ── 도메인 메서드 ─────────────────────────────────────────────────

    /**
     * 외부 발송 시스템 INSERT 완료 처리.
     *
     * @param externalMsgId 외부 시스템이 발급한 msg_id
     */
    public void markQueued(Long externalMsgId) {
        this.externalMsgId = externalMsgId;
        this.status = SendRequestStatus.QUEUED;
    }

    /**
     * 외부 INSERT 실패 처리 — 보상 트랜잭션 대상.
     *
     * @param reason 실패 사유
     */
    public void markFailed(String reason) {
        this.status = SendRequestStatus.FAILED;
        this.failReason = reason;
    }

    /**
     * 발송 거부 처리 (잔액 부족·정책 위반 등).
     *
     * @param reason 거부 사유
     */
    public void markCancelled(String reason) {
        this.status = SendRequestStatus.CANCELLED;
        this.failReason = reason;
    }

    /** 광고성 메시지 여부 */
    public boolean isAdvertisementMessage() {
        return "Y".equals(this.isAdvertisement);
    }
}
