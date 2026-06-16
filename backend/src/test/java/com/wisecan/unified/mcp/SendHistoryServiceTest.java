package com.wisecan.unified.mcp;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import com.wisecan.unified.service.dispatch.SendHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * SendHistoryService 단위 테스트 - 비노출 정책 및 소유권 검증 단언.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendHistoryService 단위 테스트")
class SendHistoryServiceTest {

    @InjectMocks
    private SendHistoryService sendHistoryService;

    @Mock
    private SendRequestRepository sendRequestRepository;

    @Test
    @DisplayName("list - apiKeyId 지정 시 키 범위로 조회")
    void list_withApiKeyId_queriesByApiKey() {
        given(sendRequestRepository.findByApiKeyIdOrderByCreatedAtDesc(anyLong(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                null, null, null, null, null, null);

        SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                sendHistoryService.list(1L, 10L, params, 0, 20);

        assertThat(result.totalElements()).isEqualTo(0L);
    }

    @Test
    @DisplayName("list - apiKeyId null 시 회원 전체 범위로 조회")
    void list_withoutApiKeyId_queriesByMember() {
        given(sendRequestRepository.findByMemberIdOrderByCreatedAtDesc(anyLong(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                null, null, null, null, null, null);

        SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                sendHistoryService.list(1L, null, params, 0, 20);

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("detail - 존재하지 않는 sendId 시 EntityNotFoundException")
    void detail_notFound_throwsEntityNotFoundException() {
        given(sendRequestRepository.findBySendId("UNKNOWN")).willReturn(Optional.empty());

        assertThatThrownBy(() -> sendHistoryService.detail(1L, 10L, "UNKNOWN"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("detail - 타인 소유 발송건 조회 시 IllegalArgumentException")
    void detail_otherMembersSend_throwsIllegalArgumentException() {
        SendRequest entity = stubSendRequest(1L, 10L, "01ARZ3NDEKTSV4RRFFQ69G5FAV");
        given(sendRequestRepository.findBySendId("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .willReturn(Optional.of(entity));

        assertThatThrownBy(() -> sendHistoryService.detail(2L, null, "01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 발송 이력만");
    }

    @Test
    @DisplayName("detail - 다른 키의 발송건 조회 시 IllegalArgumentException")
    void detail_wrongApiKey_throwsIllegalArgumentException() {
        SendRequest entity = stubSendRequest(1L, 10L, "01ARZ3NDEKTSV4RRFFQ69G5FAV");
        given(sendRequestRepository.findBySendId("01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .willReturn(Optional.of(entity));

        assertThatThrownBy(() -> sendHistoryService.detail(1L, 99L, "01ARZ3NDEKTSV4RRFFQ69G5FAV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 API Key");
    }

    @Test
    @DisplayName("비노출 정책 - DetailItem 에 routingMeta 필드 없음")
    void detail_responseDoesNotContainRoutingMeta() {
        var fields = Arrays.stream(SendHistoryDto.DetailItem.class.getRecordComponents())
                .map(rc -> rc.getName())
                .toList();
        assertThat(fields).doesNotContain("routingMeta");
        assertThat(fields).contains("messageBody");
    }

    @Test
    @DisplayName("비노출 정책 - ListItem 에 messageBody/routingMeta 없음")
    void list_listItemDoesNotContainSensitiveFields() {
        var fields = Arrays.stream(SendHistoryDto.ListItem.class.getRecordComponents())
                .map(rc -> rc.getName())
                .toList();
        assertThat(fields).doesNotContain("routingMeta");
        assertThat(fields).doesNotContain("messageBody");
    }

    private SendRequest stubSendRequest(Long memberId, Long apiKeyId, String sendId) {
        SendRequest entity = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .channel(SendChannel.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01098765432")
                .recipientCount(1)
                .messageBody("테스트")
                .isAdvertisement(false)
                .requestedAt(LocalDateTime.now())
                .unitCost(0L)
                .build();
        ReflectionTestUtils.setField(entity, "sendId", sendId);
        ReflectionTestUtils.setField(entity, "status", SendRequestStatus.QUEUED);
        return entity;
    }
}
