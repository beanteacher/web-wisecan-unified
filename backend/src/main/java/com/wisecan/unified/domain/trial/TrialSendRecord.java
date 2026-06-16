package com.wisecan.unified.domain.trial;

import com.wisecan.unified.domain.dispatch.SendChannel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체험 모드 가상 발송 기록 엔티티 (W-406).
 *
 * <p>체험 세션 중 발송을 시도하면 외부 송출 없이 가상 결과만 기록한다.
 * 운영 {@code send_request} 테이블과 완전 격리된 별도 테이블이다.</p>
 *
 * <p>외부 송출 차단 단언: {@code externalBlocked = true} 가 항상 세팅되어야 한다.
 * 테스트에서 이 필드로 차단 단언을 검증한다.</p>
 */
@Entity
@Table(name = "trial_send_record")
@Getter
@NoArgsConstructor
public class TrialSendRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 체험 세션 토큰 */
    @Column(name = "session_token", length = 36, nullable = false)
    private String sessionToken;

    /** 발송 채널 */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private SendChannel channel;

    /** 더미 수신번호 */
    @Column(name = "recipient_number", length = 20, nullable = false)
    private String recipientNumber;

    /** 발송 본문 (저장은 하되 외부 송출 안 함) */
    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    /**
     * 외부 송출 차단 여부 — 반드시 {@code true}.
     * 이 값이 false 이면 운영 데이터 오염이므로 단언 대상.
     */
    @Column(name = "external_blocked", nullable = false)
    private boolean externalBlocked;

    /** 가상 발송 결과 코드 (예: TRIAL_ACCEPTED) */
    @Column(name = "virtual_result_code", length = 30, nullable = false)
    private String virtualResultCode;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public TrialSendRecord(
            String sessionToken,
            SendChannel channel,
            String recipientNumber,
            String messageBody,
            boolean externalBlocked,
            String virtualResultCode
    ) {
        this.sessionToken = sessionToken;
        this.channel = channel;
        this.recipientNumber = recipientNumber;
        this.messageBody = messageBody;
        this.externalBlocked = externalBlocked;
        this.virtualResultCode = virtualResultCode;
    }
}
