package com.wisecan.unified.domain.sendernumber;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발신번호 등록 사건 이력 — append-only.
 *
 * 소유권 이관 개념 없음. 등록·심사·삭제 등 모든 사건을 1행씩 기록한다.
 * actor_member_id (회원 행위) / actor_operator_id (운영자 행위) 중 하나만 non-null.
 */
@Entity
@Table(name = "callback_log", indexes = {
    @Index(name = "idx_cblog_callback", columnList = "callback_id"),
    @Index(name = "idx_cblog_occurred", columnList = "occurred_at")
})
@Getter
@NoArgsConstructor
public class CallbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "callback_id", nullable = false)
    private Long callbackId;

    /** 역추적용 정규화 번호 (callback 삭제 후에도 조회 가능) */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CallbackLogEvent event;

    /** 회원 행위 시 non-null */
    @Column(name = "actor_member_id")
    private Long actorMemberId;

    /** 운영자 행위 시 non-null */
    @Column(name = "actor_operator_id")
    private Long actorOperatorId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "occurred_at", updatable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        this.occurredAt = LocalDateTime.now();
    }

    @Builder
    public CallbackLog(Long callbackId, String phoneNumber, CallbackLogEvent event,
                       Long actorMemberId, Long actorOperatorId, String comment) {
        this.callbackId = callbackId;
        this.phoneNumber = phoneNumber;
        this.event = event;
        this.actorMemberId = actorMemberId;
        this.actorOperatorId = actorOperatorId;
        this.comment = comment;
    }
}
