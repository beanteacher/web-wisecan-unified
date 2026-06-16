package com.wisecan.unified.domain.admin;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카카오/RCS 라우팅 매핑 엔티티 — §12.4.
 *
 * 회원별 카카오/RCS 중계사 1:1 매핑.
 * 매핑 결과는 외부 발송 시스템이 routing_meta 를 통해 사용하며,
 * 회원에게는 어떤 형태로도 노출하지 않는다.
 *
 * 유니크 제약: (member_id, channel) — 회원 + 채널당 1 매핑.
 */
@Entity
@Table(
    name = "routing_mapping",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_routing_member_channel",
        columnNames = {"member_id", "channel"}
    ),
    indexes = @Index(name = "idx_routing_member", columnList = "member_id")
)
@Getter
@NoArgsConstructor
public class RoutingMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutingChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoutingCarrier carrier;

    /** 내부 메모 (운영자 전용, 회원 비노출) */
    @Column(length = 255)
    private String memo;

    /** 마지막 변경 운영자 ID */
    @Column(name = "operator_id")
    private Long operatorId;

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
    public RoutingMapping(Long memberId, RoutingChannel channel, RoutingCarrier carrier,
                          String memo, Long operatorId) {
        this.memberId = memberId;
        this.channel = channel;
        this.carrier = carrier;
        this.memo = memo;
        this.operatorId = operatorId;
    }

    /** 중계사 변경 */
    public void updateCarrier(RoutingCarrier carrier, String memo, Long operatorId) {
        this.carrier = carrier;
        this.memo = memo;
        this.operatorId = operatorId;
    }
}
