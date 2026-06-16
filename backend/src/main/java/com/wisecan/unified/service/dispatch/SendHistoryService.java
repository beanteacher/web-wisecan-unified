package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 발송 이력 조회 서비스 (W-304 / RQ-MCP-006·007).
 *
 * 비노출 정책: routingMeta 는 응답 DTO(SendHistoryDto.ListItem / DetailItem)에 포함되지 않는다.
 * 동등성 보장 (§7.4): MCP wsc.history.list/detail 과 CLI wsc history list/detail 은 동일 메서드 호출.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SendHistoryService {

    private final SendRequestRepository sendRequestRepository;

    public SendHistoryDto.PageResponse<SendHistoryDto.ListItem> list(
            Long memberId,
            Long apiKeyId,
            SendHistoryDto.ListParams params,
            int page,
            int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SendRequest> result = apiKeyId != null
                ? sendRequestRepository.findByApiKeyIdOrderByCreatedAtDesc(apiKeyId, pageable)
                : sendRequestRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<SendRequest> filtered = result.getContent().stream()
                .filter(r -> params.fromDate() == null || !r.getCreatedAt().isBefore(params.fromDate()))
                .filter(r -> params.toDate()   == null || !r.getCreatedAt().isAfter(params.toDate()))
                .filter(r -> params.channel()  == null || r.getChannel() == params.channel())
                .filter(r -> params.callbackNumber()  == null || r.getCallbackNumber().contains(params.callbackNumber()))
                .filter(r -> params.recipientNumber() == null || r.getRecipientNumbers().contains(params.recipientNumber()))
                .filter(r -> params.status()   == null || r.getStatus() == params.status())
                .collect(Collectors.toList());

        Page<SendHistoryDto.ListItem> mapped =
                new PageImpl<>(filtered, pageable, filtered.size()).map(SendHistoryDto.ListItem::from);
        return SendHistoryDto.PageResponse.of(mapped);
    }

    public SendHistoryDto.DetailItem detail(Long memberId, Long apiKeyId, String sendId) {
        SendRequest entity = sendRequestRepository.findBySendId(sendId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "발송 이력을 찾을 수 없습니다: sendId=" + sendId));

        if (!entity.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 발송 이력만 조회할 수 있습니다.");
        }
        if (apiKeyId != null && !entity.getApiKeyId().equals(apiKeyId)) {
            throw new IllegalArgumentException("해당 API Key의 발송 이력이 아닙니다.");
        }
        return SendHistoryDto.DetailItem.from(entity);
    }
}
