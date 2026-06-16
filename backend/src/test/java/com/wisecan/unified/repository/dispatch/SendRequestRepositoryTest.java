package com.wisecan.unified.repository.dispatch;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.SendRequestStatus;
import com.wisecan.unified.domain.dispatch.encoding.SmsMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SendRequestRepository 슬라이스 테스트.
 *
 * <p>H2 인메모리 DB(Oracle 호환 모드)로 실행.
 * 테스팅.md §A. H2 Oracle 호환 모드 패턴 적용.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SendRequestRepositoryTest {

    @Autowired
    private SendRequestRepository sendRequestRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ── 픽스처 ────────────────────────────────────────────────────────

    private SendRequest buildSmsRequest(Long memberId, Long apiKeyId) {
        return SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(apiKeyId)
                .channel(SendChannel.SMS)
                .smsType(SmsMessageType.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(1)
                .messageBody("테스트 메시지")
                .isAdvertisement(false)
                .requestedAt(LocalDateTime.now())
                .unitCost(20L)
                .build();
    }

    // ── 테스트 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() — 저장 후 send_id(ULID) 26자 자동 부여")
    void save_assignsUlid() {
        SendRequest request = buildSmsRequest(1L, 1L);
        SendRequest saved = sendRequestRepository.save(request);

        assertThat(saved.getSendId()).isNotNull().hasSize(26);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("save() — 저장 후 status=PENDING")
    void save_statusIsPending() {
        SendRequest saved = sendRequestRepository.save(buildSmsRequest(1L, 1L));
        assertThat(saved.getStatus()).isEqualTo(SendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("findBySendId() — 저장한 ULID로 조회 성공")
    void findBySendId_returnsEntity() {
        SendRequest saved = sendRequestRepository.saveAndFlush(buildSmsRequest(2L, 1L));
        entityManager.clear();

        assertThat(sendRequestRepository.findBySendId(saved.getSendId()))
                .isPresent()
                .get()
                .extracting(SendRequest::getMemberId)
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("findBySendId() — 없는 ULID는 empty 반환")
    void findBySendId_unknownId_returnsEmpty() {
        assertThat(sendRequestRepository.findBySendId("00000000000000000000000000"))
                .isEmpty();
    }

    @Test
    @DisplayName("findByMemberIdOrderByCreatedAtDesc() — 회원별 페이지 조회")
    void findByMemberId_returnsPage() {
        sendRequestRepository.save(buildSmsRequest(10L, 1L));
        sendRequestRepository.save(buildSmsRequest(10L, 1L));
        sendRequestRepository.save(buildSmsRequest(99L, 1L)); // 다른 회원

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(r -> r.getMemberId().equals(10L));
    }

    @Test
    @DisplayName("existsBySendId() — 존재하는 ULID는 true")
    void existsBySendId_existing_returnsTrue() {
        SendRequest saved = sendRequestRepository.saveAndFlush(buildSmsRequest(3L, 1L));
        assertThat(sendRequestRepository.existsBySendId(saved.getSendId())).isTrue();
    }

    @Test
    @DisplayName("existsBySendId() — 없는 ULID는 false")
    void existsBySendId_unknown_returnsFalse() {
        assertThat(sendRequestRepository.existsBySendId("NONEXISTENT0000000000000000")).isFalse();
    }

    @Test
    @DisplayName("markQueued() — 상태가 QUEUED로 전이되고 externalMsgId 기록")
    void markQueued_updatesStatusAndExternalMsgId() {
        SendRequest saved = sendRequestRepository.saveAndFlush(buildSmsRequest(4L, 1L));

        saved.markQueued(9999L);
        sendRequestRepository.saveAndFlush(saved);
        entityManager.clear();

        SendRequest reloaded = sendRequestRepository.findBySendId(saved.getSendId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SendRequestStatus.QUEUED);
        assertThat(reloaded.getExternalMsgId()).isEqualTo(9999L);
    }

    @Test
    @DisplayName("markFailed() — 상태가 FAILED로 전이되고 failReason 기록")
    void markFailed_updatesStatusAndReason() {
        SendRequest saved = sendRequestRepository.saveAndFlush(buildSmsRequest(5L, 1L));

        saved.markFailed("외부 시스템 타임아웃");
        sendRequestRepository.saveAndFlush(saved);
        entityManager.clear();

        SendRequest reloaded = sendRequestRepository.findBySendId(saved.getSendId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SendRequestStatus.FAILED);
        assertThat(reloaded.getFailReason()).isEqualTo("외부 시스템 타임아웃");
    }

    @Test
    @DisplayName("totalCost — recipientCount × unitCost 자동 계산")
    void totalCost_calculatedOnBuild() {
        SendRequest request = SendRequest.builder()
                .memberId(6L)
                .apiKeyId(1L)
                .channel(SendChannel.SMS)
                .smsType(SmsMessageType.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01011111111,01022222222,01033333333")
                .recipientCount(3)
                .messageBody("다건 테스트")
                .isAdvertisement(false)
                .requestedAt(LocalDateTime.now())
                .unitCost(20L)
                .build();

        SendRequest saved = sendRequestRepository.save(request);
        assertThat(saved.getTotalCost()).isEqualTo(60L); // 3 × 20
    }
}
