package com.wisecan.unified.dto.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;

import java.time.LocalDateTime;

/**
 * 발송 이력 조회 전용 DTO (W-304).
 *
 * <p>SendRequestDto 와 별도 파일로 격리하여 다른 워커의 SendRequest 관련 작업과 충돌을 방지한다.</p>
 *
 * <p>키별 조회 범위 정책 (02_FEATURE_SPEC.md §5.3, §8.1):</p>
 * <ul>
 *   <li>scope:key (기본) — 해당 API Key로 발송한 이력만 반환</li>
 *   <li>scope:member (옵션) — 회원 전체 키의 이력 반환 (MEMBER_SCOPE 스코프 보유 시만)</li>
 *   <li>테스트 키 — 테스트망 발송 이력만 노출, 상용 이력 비노출</li>
 *   <li>상용 키 — 상용망 발송 이력만 노출</li>
 * </ul>
 */
public class SendHistoryDto {

    /**
     * 이력 목록 조회 파라미터.
     *
     * <p>기간·채널·발신번호·수신번호 필터 + 페이지네이션 (02 §8.1).</p>
     */
    public record ListParams(
            /** 조회 시작일시 (null = 제한 없음) */
            LocalDateTime fromDate,
            /** 조회 종료일시 (null = 제한 없음) */
            LocalDateTime toDate,
            /** 채널 필터 (null = 전체) */
            SendChannel channel,
            /** 발신번호 필터 (null = 전체) */
            String callbackNumber,
            /** 수신번호 포함 검색 (null = 전체) */
            String recipientNumber,
            /** 상태 필터 (null = 전체) */
            SendRequestStatus status
    ) {}

    /**
     * 이력 목록 항목 — 민감 정보(routingMeta, messageBody 원문) 비포함.
     */
    public record ListItem(
            String sendId,
            SendChannel channel,
            String callbackNumber,
            int recipientCount,
            SendRequestStatus status,
            long totalCost,
            LocalDateTime requestedAt,
            LocalDateTime createdAt
    ) {
        public static ListItem from(SendRequest entity) {
            return new ListItem(
                    entity.getSendId(),
                    entity.getChannel(),
                    entity.getCallbackNumber(),
                    entity.getRecipientCount(),
                    entity.getStatus(),
                    entity.getTotalCost(),
                    entity.getRequestedAt(),
                    entity.getCreatedAt()
            );
        }
    }

    /**
     * 이력 상세 응답 — routingMeta 비포함 (INV-02 회원 비노출).
     */
    public record DetailItem(
            String sendId,
            SendChannel channel,
            String callbackNumber,
            String recipientNumbers,
            int recipientCount,
            String subject,
            String messageBody,
            SendRequestStatus status,
            String failReason,
            long unitCost,
            long totalCost,
            Long externalMsgId,
            LocalDateTime requestedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static DetailItem from(SendRequest entity) {
            return new DetailItem(
                    entity.getSendId(),
                    entity.getChannel(),
                    entity.getCallbackNumber(),
                    entity.getRecipientNumbers(),
                    entity.getRecipientCount(),
                    entity.getSubject(),
                    entity.getMessageBody(),
                    entity.getStatus(),
                    entity.getFailReason(),
                    entity.getUnitCost(),
                    entity.getTotalCost(),
                    entity.getExternalMsgId(),
                    entity.getRequestedAt(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }

    /**
     * 페이지 응답 래퍼.
     */
    public record PageResponse<T>(
            java.util.List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }
}
