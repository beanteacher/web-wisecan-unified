package com.wisecan.unified.service.trial;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.trial.TrialDummyContext;
import com.wisecan.unified.domain.trial.TrialSendRecord;
import com.wisecan.unified.domain.trial.TrialSession;
import com.wisecan.unified.dto.trial.TrialDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.trial.TrialDummyContextRepository;
import com.wisecan.unified.repository.trial.TrialSendRecordRepository;
import com.wisecan.unified.repository.trial.TrialSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * TrialSessionService 단위 테스트 (W-406 TDD).
 *
 * <h3>핵심 DoD 검증 항목</h3>
 * <ol>
 *   <li>체험 발송 시 {@code externalBlocked = true} 단언 — 외부 송출 완전 차단</li>
 *   <li>결제 차단 응답이 {@code TRIAL_MODE} reason 포함</li>
 *   <li>어뷰징 임계치 초과 시 {@link TrialAbuseBlockedException} 발생</li>
 *   <li>만료 세션으로 발송 시도 시 {@link TrialSessionExpiredException} 발생</li>
 *   <li>운영 {@code send_request} 테이블에 기록 없음 — {@code TrialSendRecordRepository} 만 호출</li>
 *   <li>세션 발급 시 더미 컨텍스트가 함께 저장됨</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class TrialSessionServiceTest {

    @Mock
    private TrialSessionRepository trialSessionRepository;

    @Mock
    private TrialDummyContextRepository trialDummyContextRepository;

    @Mock
    private TrialSendRecordRepository trialSendRecordRepository;

    @Mock
    private TrialDummyDataFactory dummyDataFactory;

    @InjectMocks
    private TrialSessionService trialSessionService;

    // ── 세션 발급 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("세션 발급 성공 — 세션 토큰 + 더미 컨텍스트 동시 저장")
    void issueSession_success_savesSessionAndDummyContext() {
        // given
        String clientIp = "192.168.0.1";
        given(trialSessionRepository.countByClientIpSince(eq(clientIp), any(LocalDateTime.class)))
                .willReturn(0L);
        given(trialSessionRepository.save(any(TrialSession.class)))
                .willAnswer(inv -> inv.getArgument(0));

        TrialDummyContext dummyCtx = TrialDummyContext.builder()
                .sessionToken("any-token")
                .dummyCallbackNumber("010-1234-5678")
                .dummyApiKey("wsc_test_****_TRIAL")
                .dummyBalance(50_000L)
                .dummySendHistoryJson("[]")
                .dummyKakaoTemplateJson("[]")
                .dummyRcsBrandJson("{}")
                .build();
        given(dummyDataFactory.buildDummyContext(any(String.class))).willReturn(dummyCtx);
        given(trialDummyContextRepository.save(any(TrialDummyContext.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        TrialDto.SessionResponse response = trialSessionService.issueSession(clientIp);

        // then
        assertThat(response.sessionToken()).isNotBlank();
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
        then(trialSessionRepository).should().save(any(TrialSession.class));
        then(trialDummyContextRepository).should().save(any(TrialDummyContext.class));
    }

    @Test
    @DisplayName("어뷰징 임계치(5회) 초과 시 TrialAbuseBlockedException 발생")
    void issueSession_abuseExceeded_throwsException() {
        // given
        String clientIp = "10.0.0.1";
        given(trialSessionRepository.countByClientIpSince(eq(clientIp), any(LocalDateTime.class)))
                .willReturn(5L); // 임계치 = 5, 이미 5회 → 차단

        // when / then
        assertThatThrownBy(() -> trialSessionService.issueSession(clientIp))
                .isInstanceOf(TrialAbuseBlockedException.class)
                .hasMessageContaining("한도를 초과");
    }

    // ── 외부 송출 차단 단언 (핵심 DoD) ────────────────────────────────

    @Test
    @DisplayName("체험 발송 시 externalBlocked = true 단언 — 외부 송출 완전 차단")
    void trialSend_externalBlockedAlwaysTrue() {
        // given
        String token = "test-session-token";
        TrialSession activeSession = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(activeSession));

        ArgumentCaptor<TrialSendRecord> captor = ArgumentCaptor.forClass(TrialSendRecord.class);
        given(trialSendRecordRepository.save(captor.capture()))
                .willAnswer(inv -> inv.getArgument(0));

        TrialDto.SendRequest request = new TrialDto.SendRequest(
                SendChannel.SMS, "010-9876-5432", "체험 발송 테스트 메시지");

        // when
        TrialDto.SendResponse response = trialSessionService.trialSend(token, request);

        // then — 핵심 DoD: externalBlocked 반드시 true
        TrialSendRecord saved = captor.getValue();
        assertThat(saved.isExternalBlocked())
                .as("외부 송출 차단 단언: externalBlocked must be true")
                .isTrue();
        assertThat(saved.getVirtualResultCode()).isEqualTo("TRIAL_ACCEPTED");
        assertThat(response.externalBlocked()).isTrue();
        assertThat(response.message()).contains("실제 발송");
    }

    @Test
    @DisplayName("체험 발송 시 운영 send_request 테이블에 기록 없음 — TrialSendRecordRepository만 호출")
    void trialSend_doesNotWriteToOperationalSendRequest() {
        // given
        String token = "test-session-token-2";
        TrialSession activeSession = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(activeSession));
        given(trialSendRecordRepository.save(any(TrialSendRecord.class)))
                .willAnswer(inv -> inv.getArgument(0));

        TrialDto.SendRequest request = new TrialDto.SendRequest(
                SendChannel.KAKAO, "010-1111-2222", "카카오 체험 메시지");

        // when
        trialSessionService.trialSend(token, request);

        // then — SendRequestRepository(운영 테이블)는 절대 호출되지 않음
        then(trialSendRecordRepository).should().save(any(TrialSendRecord.class));
        // SendRequestRepository를 @Mock으로 주입하지 않았으므로 호출 자체가 불가 — 격리 보증
    }

    @Test
    @DisplayName("만료된 세션으로 발송 시도 시 TrialSessionExpiredException 발생")
    void trialSend_expiredSession_throwsException() {
        // given
        String token = "expired-token";
        TrialSession expiredSession = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // 이미 만료
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(expiredSession));

        TrialDto.SendRequest request = new TrialDto.SendRequest(
                SendChannel.SMS, "010-0000-0000", "만료 세션 테스트");

        // when / then
        assertThatThrownBy(() -> trialSessionService.trialSend(token, request))
                .isInstanceOf(TrialSessionExpiredException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 발송 시도 시 EntityNotFoundException 발생")
    void trialSend_unknownSession_throwsEntityNotFoundException() {
        // given
        given(trialSessionRepository.findById("unknown-token")).willReturn(Optional.empty());

        TrialDto.SendRequest request = new TrialDto.SendRequest(
                SendChannel.SMS, "010-0000-0000", "없는 세션");

        // when / then
        assertThatThrownBy(() -> trialSessionService.trialSend("unknown-token", request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── 결제 차단 단언 (핵심 DoD) ─────────────────────────────────────

    @Test
    @DisplayName("체험 결제 시도 시 TRIAL_MODE reason 포함 차단 응답 반환")
    void blockBilling_returnsBlockedResponse() {
        // given
        String token = "billing-session-token";
        TrialSession activeSession = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(activeSession));

        // when
        TrialDto.BillingBlockedResponse response = trialSessionService.blockBilling(token);

        // then — 핵심 DoD: 결제 차단 reason = TRIAL_MODE
        assertThat(response.reason()).isEqualTo("TRIAL_MODE");
        assertThat(response.message()).contains("결제·충전은 일어나지 않습니다");
    }

    @Test
    @DisplayName("만료된 세션으로 결제 시도 시 TrialSessionExpiredException 발생")
    void blockBilling_expiredSession_throwsException() {
        // given
        String token = "expired-billing-token";
        TrialSession expiredSession = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(expiredSession));

        // when / then
        assertThatThrownBy(() -> trialSessionService.blockBilling(token))
                .isInstanceOf(TrialSessionExpiredException.class);
    }

    // ── 세션 종료 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("세션 종료 — closedAt 기록 (soft 폐기)")
    void closeSession_setsClosedAt() {
        // given
        String token = "close-session-token";
        TrialSession session = TrialSession.builder()
                .sessionToken(token)
                .clientIp("127.0.0.1")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        given(trialSessionRepository.findById(token)).willReturn(Optional.of(session));

        // when
        trialSessionService.closeSession(token);

        // then
        assertThat(session.getClosedAt()).isNotNull();
        assertThat(session.isActive()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 세션 종료 시도 시 EntityNotFoundException 발생")
    void closeSession_unknownSession_throwsEntityNotFoundException() {
        // given
        given(trialSessionRepository.findById("ghost-token")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> trialSessionService.closeSession("ghost-token"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
