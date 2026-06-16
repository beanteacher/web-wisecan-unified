package com.wisecan.unified.domain.template;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SMS17 이관 처리 큐.
 *
 * 02_FEATURE_SPEC §9.1 "기존 SMS17 이관 시 운영자 처리 큐 진입" 참조.
 * 카카오 템플릿의 SMS17 → WiseCan 이관 신청을 관리한다.
 * 운영자가 /admin/review/template-transfer 에서 처리한다.
 */
@Entity
@Table(name = "template_transfer_queue")
@Getter
@NoArgsConstructor
public class TemplateTransferQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 회원 ID */
    @Column(nullable = false)
    private Long memberId;

    /** 이관 대상 템플릿 코드 (SMS17 측 코드) */
    @Column(nullable = false, length = 50)
    private String sourceTemplateCode;

    /** 이관 대상 카카오 프로필(브랜드) 번호 */
    @Column
    private Integer kkoProfileNo;

    /** 이관 신청 사유 */
    @Column(length = 500)
    private String reason;

    /** 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemplateTransferStatus status;

    /** 반려 사유 */
    @Column(length = 500)
    private String rejectReason;

    /** 처리 운영자 ID */
    @Column
    private Long operatorId;

    /** 신청 시각 */
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    /** 처리 완료 시각 */
    @Column
    private LocalDateTime resolvedAt;

    @Builder
    public TemplateTransferQueue(Long memberId, String sourceTemplateCode,
                                 Integer kkoProfileNo, String reason) {
        this.memberId = memberId;
        this.sourceTemplateCode = sourceTemplateCode;
        this.kkoProfileNo = kkoProfileNo;
        this.reason = reason;
        this.status = TemplateTransferStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    /** 운영자 처리 시작 */
    public void startProcess(Long operatorId) {
        if (this.status != TemplateTransferStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태만 처리 시작 가능합니다: " + this.status);
        }
        this.status = TemplateTransferStatus.IN_PROGRESS;
        this.operatorId = operatorId;
    }

    /** 이관 완료 처리 */
    public void complete(Long operatorId) {
        if (this.status != TemplateTransferStatus.IN_PROGRESS
                && this.status != TemplateTransferStatus.PENDING) {
            throw new IllegalStateException("이관 완료 처리 불가 상태입니다: " + this.status);
        }
        this.status = TemplateTransferStatus.COMPLETED;
        this.operatorId = operatorId;
        this.resolvedAt = LocalDateTime.now();
    }

    /** 이관 거부 처리 */
    public void reject(Long operatorId, String rejectReason) {
        if (this.status.isTerminal()) {
            throw new IllegalStateException("이미 종료된 이관 신청입니다: " + this.status);
        }
        this.status = TemplateTransferStatus.REJECTED;
        this.operatorId = operatorId;
        this.rejectReason = rejectReason;
        this.resolvedAt = LocalDateTime.now();
    }

    /** 신청자 취소 */
    public void cancel() {
        if (this.status != TemplateTransferStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태만 취소 가능합니다: " + this.status);
        }
        this.status = TemplateTransferStatus.CANCELLED;
        this.resolvedAt = LocalDateTime.now();
    }
}
