package com.wisecan.unified.service.trial;

import com.wisecan.unified.domain.trial.TrialDummyContext;
import com.wisecan.unified.domain.trial.TrialSendRecord;
import com.wisecan.unified.domain.trial.TrialSession;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.dto.trial.TrialDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.trial.TrialDummyContextRepository;
import com.wisecan.unified.repository.trial.TrialSendRecordRepository;
import com.wisecan.unified.repository.trial.TrialSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 체험 모드 서비스 (W-406).
 *
 * <p>책임:</p>
 * <ol>
 *   <li>체험 세션 발급 (어뷰징 차단 임계치 확인 → 세션 토큰 발급 → 더미 컨텍스트 사전 적재)</li>
 *   <li>체험 발송 시도 처리 — 외부 송출 완전 차단 + {@link TrialSendRecord#isExternalBlocked()} = true 단언</li>
 *   <li>체험 결제/충전 시도 차단 — 즉시 {@link TrialDto.BillingBlockedResponse#blocked()} 반환</li>
 *   <li>세션 종료 — 더미 데이터 soft 폐기(closedAt 기록)</li>
 * </ol>
 *
 * <p>운영 테이블(send_request, cash_balance 등) 에는 단 한 줄도 기록하지 않는다.
 * 모든 체험 데이터는 {@code trial_*} 테이블에만 적재된다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrialSessionService {

    /** 어뷰징 차단 임계치: 동일 IP 기준 10분 내 최대 세션 수 */
    private static final int ABUSE_LIMIT = 5;
    /** 어뷰징 판별 시간 창 (분) */
    private static final int ABUSE_WINDOW_MINUTES = 10;
    /** 세션 유효 시간 (분) */
    private static final int SESSION_TTL_MINUTES = 30;

    private final TrialSessionRepository trialSessionRepository;
    private final TrialDummyContextRepository trialDummyContextRepository;
    private final TrialSendRecordRepository trialSendRecordRepository;
    private final TrialDummyDataFactory dummyDataFactory;

    // ── 세션 발급 ──────────────────────────────────────────────────────

    /**
     * 체험 세션을 발급하고 더미 컨텍스트를 사전 적재한다.
     *
     * @param clientIp 요청 클라이언트 IP
     * @return 세션 토큰 + 만료 일시 + 더미 컨텍스트 요약
     * @throws TrialAbuseBlockedException 어뷰징 임계치 초과 시
     */
    @Transactional(rollbackFor = Exception.class)
    public TrialDto.SessionResponse issueSession(String clientIp) {
        checkAbuseLimit(clientIp);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(SESSION_TTL_MINUTES);

        TrialSession session = TrialSession.builder()
                .sessionToken(token)
                .clientIp(clientIp)
                .expiresAt(expiresAt)
                .build();
        trialSessionRepository.save(session);

        TrialDummyContext ctx = dummyDataFactory.buildDummyContext(token);
        trialDummyContextRepository.save(ctx);

        log.info("[Trial] 세션 발급 — token={}, ip={}, expiresAt={}", token, clientIp, expiresAt);
        return TrialDto.SessionResponse.of(session, ctx);
    }

    // ── 체험 발송 ──────────────────────────────────────────────────────

    /**
     * 체험 발송을 처리한다.
     *
     * <p>외부 송출은 완전 차단되며 {@link TrialSendRecord#isExternalBlocked()} 는
     * 반드시 {@code true} 이다. 이 단언이 깨지면 운영 데이터 오염이므로
     * 단위 테스트에서 검증한다.</p>
     *
     * @param sessionToken 체험 세션 토큰
     * @param request      발송 요청 DTO
     * @return 가상 발송 결과
     */
    @Transactional(rollbackFor = Exception.class)
    public TrialDto.SendResponse trialSend(String sessionToken, TrialDto.SendRequest request) {
        TrialSession session = loadActiveSession(sessionToken);

        // 외부 송출 차단 단언: externalBlocked = true (절대 false 불가)
        TrialSendRecord record = TrialSendRecord.builder()
                .sessionToken(session.getSessionToken())
                .channel(request.channel())
                .recipientNumber(request.recipientNumber())
                .messageBody(request.messageBody())
                .externalBlocked(true)           // ← 핵심 차단 단언 필드
                .virtualResultCode("TRIAL_ACCEPTED")
                .build();
        TrialSendRecord saved = trialSendRecordRepository.save(record);

        log.info("[Trial] 가상 발송 기록 — sessionToken={}, channel={}, externalBlocked={}",
                sessionToken, request.channel(), saved.isExternalBlocked());

        return TrialDto.SendResponse.from(saved);
    }

    // ── 체험 결제 차단 ─────────────────────────────────────────────────

    /**
     * 체험 모드 결제/충전 시도를 차단한다.
     *
     * <p>실제 PG 연동 없이 즉시 차단 응답을 반환한다.
     * billing 서비스가 체험 세션을 감지하면 이 메서드를 호출한다.</p>
     *
     * @param sessionToken 체험 세션 토큰
     * @return 결제 차단 응답
     */
    public TrialDto.BillingBlockedResponse blockBilling(String sessionToken) {
        loadActiveSession(sessionToken); // 유효 세션 검증
        log.info("[Trial] 결제 차단 — sessionToken={}", sessionToken);
        return TrialDto.BillingBlockedResponse.blocked();
    }

    // ── 세션 종료 ──────────────────────────────────────────────────────

    /**
     * 체험 세션을 종료하고 더미 데이터를 soft 폐기한다.
     *
     * <p>가입 전환 또는 사용자 명시 종료 시 호출한다.
     * 더미 데이터는 이관되지 않는다 (02 §2.3).</p>
     *
     * @param sessionToken 체험 세션 토큰
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeSession(String sessionToken) {
        TrialSession session = trialSessionRepository.findById(sessionToken)
                .orElseThrow(() -> new EntityNotFoundException("체험 세션을 찾을 수 없습니다: " + sessionToken));
        session.close();
        log.info("[Trial] 세션 종료 — sessionToken={}", sessionToken);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────

    private void checkAbuseLimit(String clientIp) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(ABUSE_WINDOW_MINUTES);
        long count = trialSessionRepository.countByClientIpSince(clientIp, since);
        if (count >= ABUSE_LIMIT) {
            throw new TrialAbuseBlockedException(
                    "체험 세션 발급 한도를 초과했습니다. 잠시 후 다시 시도해 주세요. ip=" + clientIp);
        }
    }

    private TrialSession loadActiveSession(String sessionToken) {
        TrialSession session = trialSessionRepository.findById(sessionToken)
                .orElseThrow(() -> new EntityNotFoundException("체험 세션을 찾을 수 없습니다: " + sessionToken));
        if (!session.isActive()) {
            throw new TrialSessionExpiredException("체험 세션이 만료되었습니다: " + sessionToken);
        }
        return session;
    }
}
