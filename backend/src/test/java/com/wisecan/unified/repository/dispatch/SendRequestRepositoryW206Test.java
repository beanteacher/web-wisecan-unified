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
 * W-206 추가 쿼리 메서드 — SendRequestRepository 슬라이스 테스트.
 *
 * <p>H2 인메모리 DB(Oracle 호환 모드)로 실행.
 * {@code findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc} 검증.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SendRequestRepositoryW206Test {

    @Autowired
    private SendRequestRepository sendRequestRepository;

    @Autowired
    private TestEntityManager entityManager;

    // ── 픽스처 ────────────────────────────────────────────────────────

    private SendRequest buildRequest(Long memberId, SendRequestStatus status, LocalDateTime requestedAt) {
        SendRequest req = SendRequest.builder()
                .memberId(memberId)
                .apiKeyId(10L)
                .channel(SendChannel.SMS)
                .smsType(SmsMessageType.SMS)
                .callbackNumber("01012345678")
                .recipientNumbers("01099999999")
                .recipientCount(1)
                .messageBody("테스트 예약 메시지")
                .isAdvertisement(false)
                .requestedAt(requestedAt)
                .unitCost(20L)
                .build();
        // 상태 전이: QUEUED/FAILED/CANCELLED는 도메인 메서드 사용
        if (status == SendRequestStatus.QUEUED) {
            req.markQueued(9999L);
        } else if (status == SendRequestStatus.FAILED) {
            req.markFailed("테스트 실패");
        } else if (status == SendRequestStatus.CANCELLED) {
            req.markCancelled("테스트 취소");
        }
        // PENDING은 초기값 유지
        return req;
    }

    // ── findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc ──

    @Test
    @DisplayName("미래 PENDING 2건 저장 → after=now() 기준 조회 시 2건 반환")
    void findPendingScheduled_twoFutureEntries_returnsBoth() {
        LocalDateTime now = LocalDateTime.now();
        sendRequestRepository.save(buildRequest(1L, SendRequestStatus.PENDING, now.plusHours(1)));
        sendRequestRepository.save(buildRequest(1L, SendRequestStatus.PENDING, now.plusHours(2)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        1L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allMatch(r -> r.getMemberId().equals(1L))
                .allMatch(r -> r.getStatus() == SendRequestStatus.PENDING)
                .allMatch(r -> r.getRequestedAt().isAfter(now));
    }

    @Test
    @DisplayName("과거 requestedAt PENDING → after=now() 기준 조회 시 제외")
    void findPendingScheduled_pastRequestedAt_excluded() {
        LocalDateTime now = LocalDateTime.now();
        // 과거 (예약 시각 지남)
        sendRequestRepository.save(buildRequest(2L, SendRequestStatus.PENDING, now.minusHours(1)));
        // 미래
        sendRequestRepository.save(buildRequest(2L, SendRequestStatus.PENDING, now.plusHours(1)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        2L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getRequestedAt()).isAfter(now);
    }

    @Test
    @DisplayName("QUEUED 상태 → PENDING 조건 조회 시 제외")
    void findPendingScheduled_queuedStatus_excluded() {
        LocalDateTime now = LocalDateTime.now();
        sendRequestRepository.save(buildRequest(3L, SendRequestStatus.QUEUED, now.plusHours(1)));
        sendRequestRepository.save(buildRequest(3L, SendRequestStatus.PENDING, now.plusHours(2)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        3L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(SendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("다른 memberId → 조회 시 제외")
    void findPendingScheduled_differentMemberId_excluded() {
        LocalDateTime now = LocalDateTime.now();
        // memberId=4 의 예약
        sendRequestRepository.save(buildRequest(4L, SendRequestStatus.PENDING, now.plusHours(1)));
        // memberId=99 의 예약
        sendRequestRepository.save(buildRequest(99L, SendRequestStatus.PENDING, now.plusHours(1)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        4L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getMemberId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("정렬 — requestedAt 오름차순 반환")
    void findPendingScheduled_sortedByRequestedAtAsc() {
        LocalDateTime now = LocalDateTime.now();
        // 늦은 시각 먼저 저장
        sendRequestRepository.save(buildRequest(5L, SendRequestStatus.PENDING, now.plusHours(3)));
        sendRequestRepository.save(buildRequest(5L, SendRequestStatus.PENDING, now.plusHours(1)));
        sendRequestRepository.save(buildRequest(5L, SendRequestStatus.PENDING, now.plusHours(2)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        5L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(3);
        LocalDateTime first  = page.getContent().get(0).getRequestedAt();
        LocalDateTime second = page.getContent().get(1).getRequestedAt();
        LocalDateTime third  = page.getContent().get(2).getRequestedAt();
        assertThat(first).isBefore(second);
        assertThat(second).isBefore(third);
    }

    @Test
    @DisplayName("페이징 — size=1 첫 페이지: totalElements=3, content 1건")
    void findPendingScheduled_paginationFirstPage() {
        LocalDateTime now = LocalDateTime.now();
        sendRequestRepository.save(buildRequest(6L, SendRequestStatus.PENDING, now.plusHours(1)));
        sendRequestRepository.save(buildRequest(6L, SendRequestStatus.PENDING, now.plusHours(2)));
        sendRequestRepository.save(buildRequest(6L, SendRequestStatus.PENDING, now.plusHours(3)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        6L, SendRequestStatus.PENDING, now, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("취소된 건 → 조회 제외")
    void findPendingScheduled_cancelledEntry_excluded() {
        LocalDateTime now = LocalDateTime.now();
        sendRequestRepository.save(buildRequest(7L, SendRequestStatus.CANCELLED, now.plusHours(1)));
        entityManager.flush();
        entityManager.clear();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        7L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("조회 결과 없으면 빈 Page 반환")
    void findPendingScheduled_noMatch_returnsEmptyPage() {
        LocalDateTime now = LocalDateTime.now();

        Page<SendRequest> page = sendRequestRepository
                .findByMemberIdAndStatusAndRequestedAtAfterOrderByRequestedAtAsc(
                        999L, SendRequestStatus.PENDING, now, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }
}
