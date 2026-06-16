package com.wisecan.unified.domain.sendernumber;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 발신번호 엔티티 — 4 케이스(SELF_MOBILE / SELF_LANDLINE / EMPLOYEE / CORP_REP) 단일 수용.
 *
 * 활성 등록 1개 강제: 동일 phone_number 에 status ∈ {SUBMITTED, UNDER_REVIEW, REGISTERED} 행은 1개만.
 * 소유권 이관 개념 없음 — 등록·삭제 사건은 CallbackLog 에 기록.
 */
@Entity
@Table(name = "callback", indexes = {
    @Index(name = "idx_callback_member", columnList = "member_id"),
    @Index(name = "idx_callback_company", columnList = "company_id"),
    @Index(name = "idx_callback_status", columnList = "status"),
    @Index(name = "idx_callback_phone_status", columnList = "phone_number, status")
})
@Getter
@NoArgsConstructor
public class Callback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 등록자 회원 ID (법인 대표번호는 nullable — company_id 로 소유) */
    @Column(name = "member_id")
    private Long memberId;

    /** 회사 ID (EMPLOYEE / CORP_REP 케이스, nullable) */
    @Column(name = "company_id")
    private Long companyId;

    /** E.164 정규화된 전화번호 */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** 등록 케이스 4종 */
    @Enumerated(EnumType.STRING)
    @Column(name = "register_type", nullable = false, length = 20)
    private CallbackRegisterType registerType;

    /** 용도 설명 (회원 입력, 예: "고객 문자 발송용") */
    @Column(name = "description", length = 100)
    private String description;

    /** 발신번호 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallbackStatus status;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Callback(Long memberId, Long companyId, String phoneNumber,
                    CallbackRegisterType registerType, String description,
                    CallbackStatus status) {
        this.memberId = memberId;
        this.companyId = companyId;
        this.phoneNumber = phoneNumber;
        this.registerType = registerType;
        this.description = description;
        this.status = status;
    }

    // ── 도메인 메서드 ──────────────────────────────────────────────

    /** SELF_MOBILE / SELF_LANDLINE: 즉시 등록 확정 */
    public void registerImmediately() {
        this.status = CallbackStatus.REGISTERED;
        this.registeredAt = LocalDateTime.now();
    }

    /** EMPLOYEE / CORP_REP: 운영자 심사 큐로 전이 */
    public void submitForReview() {
        this.status = CallbackStatus.UNDER_REVIEW;
    }

    /** 운영자 승인 */
    public void approve() {
        this.status = CallbackStatus.REGISTERED;
        this.registeredAt = LocalDateTime.now();
    }

    /** 운영자 반려 */
    public void reject(String reason) {
        this.status = CallbackStatus.REJECTED;
        this.rejectReason = reason;
    }

    /** 삭제 (회원 직접 삭제 또는 연쇄 삭제) */
    public void delete() {
        this.status = CallbackStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /** 활성 상태 여부 (SUBMITTED / UNDER_REVIEW / REGISTERED) */
    public boolean isActive() {
        return this.status == CallbackStatus.SUBMITTED
            || this.status == CallbackStatus.UNDER_REVIEW
            || this.status == CallbackStatus.REGISTERED;
    }

    /** 발송 가능 여부 */
    public boolean isRegistered() {
        return this.status == CallbackStatus.REGISTERED;
    }
}
