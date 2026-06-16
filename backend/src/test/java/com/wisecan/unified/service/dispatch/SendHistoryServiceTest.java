package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.dto.dispatch.SendHistoryDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * SendHistoryService 단위 테스트 (W-304).
 *
 * <p>키별 조회 범위 정책 검증이 핵심 DoD:</p>
 * <ul>
 *   <li>scope:key (apiKeyId != null) — 해당 키 이력만 반환</li>
 *   <li>scope:member (apiKeyId == null) — memberId 기준 전체 키 이력 반환</li>
 *   <li>상세 — 동일 memberId 인 경우에만 접근 허용, 다른 member 는 EntityNotFoundException(404)</li>
 *   <li>scope:key 상세 — 동일 apiKeyId 아닌 경우 EntityNotFoundException(404)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SendHistoryServiceTest {

    @Mock
    private SendRequestRepository sendRequestRepository;

    @InjectMocks
    private SendHistoryService sendHistoryService;

    private static final Long API_KEY_ID      = 1L;
    private static final Long MEMBER_ID       = 10L;
    private static final Long OTHER_KEY_ID    = 99L;
    private static final Long OTHER_MEMBER_ID = 99L;

    private SendHistoryDto.ListParams emptyParams;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        emptyParams = new SendHistoryDto.ListParams(null, null, null, null, null, null);
        pageable    = PageRequest.of(0, 20);
    }

    // ── 목록 조회: scope:key ─────────────────────────────────────────────

    @Nested
    @DisplayName("목록 조회 — scope:key (apiKeyId 전달)")
    class ListScopeKey {

        @Test
        @DisplayName("apiKeyId 기준으로 이력을 조회한다")
        void list_scopeKey_queriesByApiKeyId() {
            // given
            SendRequest req = buildSendRequest(API_KEY_ID, MEMBER_ID);
            Page<SendRequest> page = new PageImpl<>(List.of(req));
            given(sendRequestRepository.findByApiKeyIdOrderByCreatedAtDesc(eq(API_KEY_ID), any(Pageable.class)))
                    .willReturn(page);

            // when
            SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                    sendHistoryService.list(MEMBER_ID, API_KEY_ID, emptyParams, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).sendId()).isEqualTo(req.getSendId());
            verify(sendRequestRepository).findByApiKeyIdOrderByCreatedAtDesc(eq(API_KEY_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("채널 필터가 일치하는 이력만 반환한다")
        void list_scopeKey_withChannelFilter_returnsFiltered() {
            // given
            SendRequest sms = buildSendRequest(API_KEY_ID, MEMBER_ID, SendChannel.SMS);
            SendRequest lms = buildSendRequest(API_KEY_ID, MEMBER_ID, SendChannel.LMS);
            Page<SendRequest> page = new PageImpl<>(List.of(sms, lms));
            given(sendRequestRepository.findByApiKeyIdOrderByCreatedAtDesc(eq(API_KEY_ID), any(Pageable.class)))
                    .willReturn(page);

            SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                    null, null, SendChannel.SMS, null, null, null);

            // when
            SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                    sendHistoryService.list(MEMBER_ID, API_KEY_ID, params, 0, 20);

            // then — SMS 만 반환
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).channel()).isEqualTo(SendChannel.SMS);
        }

        @Test
        @DisplayName("상태 필터가 일치하는 이력만 반환한다")
        void list_scopeKey_withStatusFilter_returnsFiltered() {
            // given
            SendRequest queued = buildSendRequestWithStatus(API_KEY_ID, MEMBER_ID, SendRequestStatus.QUEUED);
            SendRequest failed = buildSendRequestWithStatus(API_KEY_ID, MEMBER_ID, SendRequestStatus.FAILED);
            Page<SendRequest> page = new PageImpl<>(List.of(queued, failed));
            given(sendRequestRepository.findByApiKeyIdOrderByCreatedAtDesc(eq(API_KEY_ID), any(Pageable.class)))
                    .willReturn(page);

            SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                    null, null, null, null, null, SendRequestStatus.QUEUED);

            // when
            SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                    sendHistoryService.list(MEMBER_ID, API_KEY_ID, params, 0, 20);

            // then — QUEUED 만 반환
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).status()).isEqualTo(SendRequestStatus.QUEUED);
        }
    }

    // ── 목록 조회: scope:member ──────────────────────────────────────────

    @Nested
    @DisplayName("목록 조회 — scope:member (apiKeyId = null)")
    class ListScopeMember {

        @Test
        @DisplayName("apiKeyId=null이면 memberId 기준으로 전체 키 이력을 조회한다")
        void list_scopeMember_queriesByMemberId() {
            // given
            SendRequest req = buildSendRequest(OTHER_KEY_ID, MEMBER_ID); // 다른 키, 같은 회원
            Page<SendRequest> page = new PageImpl<>(List.of(req));
            given(sendRequestRepository.findByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(page);

            // when — apiKeyId = null → scope:member 경로
            SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                    sendHistoryService.list(MEMBER_ID, null, emptyParams, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            verify(sendRequestRepository).findByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("scope:member에서도 상태 필터가 적용된다")
        void list_scopeMember_withStatusFilter() {
            // given
            SendRequest queued = buildSendRequestWithStatus(OTHER_KEY_ID, MEMBER_ID, SendRequestStatus.QUEUED);
            SendRequest failed = buildSendRequestWithStatus(API_KEY_ID, MEMBER_ID, SendRequestStatus.FAILED);
            Page<SendRequest> page = new PageImpl<>(List.of(queued, failed));
            given(sendRequestRepository.findByMemberIdOrderByCreatedAtDesc(eq(MEMBER_ID), any(Pageable.class)))
                    .willReturn(page);

            SendHistoryDto.ListParams params = new SendHistoryDto.ListParams(
                    null, null, null, null, null, SendRequestStatus.FAILED);

            // when
            SendHistoryDto.PageResponse<SendHistoryDto.ListItem> result =
                    sendHistoryService.list(MEMBER_ID, null, params, 0, 20);

            // then — FAILED 만 반환
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).status()).isEqualTo(SendRequestStatus.FAILED);
        }
    }

    // ── 상세 조회 ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("상세 조회")
    class Detail {

        @Test
        @DisplayName("scope:key — 동일 apiKeyId 이면 상세를 반환한다")
        void detail_scopeKey_sameApiKey_returnsDetail() {
            // given
            SendRequest req = buildSendRequest(API_KEY_ID, MEMBER_ID);
            given(sendRequestRepository.findBySendId(req.getSendId()))
                    .willReturn(Optional.of(req));

            // when
            SendHistoryDto.DetailItem result =
                    sendHistoryService.detail(MEMBER_ID, API_KEY_ID, req.getSendId());

            // then
            assertThat(result.sendId()).isEqualTo(req.getSendId());
            assertThat(result.channel()).isEqualTo(SendChannel.SMS);
        }

        @Test
        @DisplayName("scope:key — 다른 apiKeyId 이면 EntityNotFoundException (404 통일)")
        void detail_scopeKey_differentApiKey_throwsEntityNotFoundException() {
            // given — 이력은 OTHER_KEY_ID 소유, 요청은 API_KEY_ID
            SendRequest req = buildSendRequest(OTHER_KEY_ID, OTHER_MEMBER_ID);
            given(sendRequestRepository.findBySendId(req.getSendId()))
                    .willReturn(Optional.of(req));

            // when & then — 존재 자체를 숨기므로 404
            assertThatThrownBy(() ->
                    sendHistoryService.detail(MEMBER_ID, API_KEY_ID, req.getSendId())
            ).isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("scope:member — 동일 memberId 이면 다른 키의 이력도 접근 가능")
        void detail_scopeMember_sameMember_differentKey_returnsDetail() {
            // given — 이력은 OTHER_KEY_ID 소유이지만 같은 회원
            SendRequest req = buildSendRequest(OTHER_KEY_ID, MEMBER_ID);
            given(sendRequestRepository.findBySendId(req.getSendId()))
                    .willReturn(Optional.of(req));

            // when — apiKeyId = null → scope:member
            SendHistoryDto.DetailItem result =
                    sendHistoryService.detail(MEMBER_ID, null, req.getSendId());

            // then
            assertThat(result.sendId()).isEqualTo(req.getSendId());
        }

        @Test
        @DisplayName("scope:member — 다른 memberId 이면 EntityNotFoundException (404 통일)")
        void detail_scopeMember_differentMember_throwsEntityNotFoundException() {
            // given — 이력은 OTHER_MEMBER 소유
            SendRequest req = buildSendRequest(OTHER_KEY_ID, OTHER_MEMBER_ID);
            given(sendRequestRepository.findBySendId(req.getSendId()))
                    .willReturn(Optional.of(req));

            // when & then
            assertThatThrownBy(() ->
                    sendHistoryService.detail(MEMBER_ID, null, req.getSendId())
            ).isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("sendId 가 존재하지 않으면 EntityNotFoundException")
        void detail_sendIdNotFound_throwsEntityNotFoundException() {
            // given
            given(sendRequestRepository.findBySendId("NOTEXIST123456789012345678"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    sendHistoryService.detail(MEMBER_ID, API_KEY_ID, "NOTEXIST123456789012345678")
            ).isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── 픽스처 헬퍼 ─────────────────────────────────────────────────────

    private SendRequest buildSendRequest(Long apiKeyId, Long memberId) {
        return buildSendRequest(apiKeyId, memberId, SendChannel.SMS);
    }

    private SendRequest buildSendRequest(Long apiKeyId, Long memberId, SendChannel channel) {
        SendRequest req = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .channel(channel)
                .smsType(null)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(1)
                .subject(null)
                .messageBody("테스트 메시지")
                .isAdvertisement(false)
                .senderKey(null)
                .templateCode(null)
                .requestedAt(LocalDateTime.now())
                .groupId(null)
                .routingMeta(null)
                .unitCost(10L)
                .build();
        ReflectionTestUtils.setField(req, "updatedAt", LocalDateTime.now());
        return req;
    }

    private SendRequest buildSendRequestWithStatus(Long apiKeyId, Long memberId, SendRequestStatus status) {
        SendRequest req = buildSendRequest(apiKeyId, memberId);
        ReflectionTestUtils.setField(req, "status", status);
        return req;
    }
}
