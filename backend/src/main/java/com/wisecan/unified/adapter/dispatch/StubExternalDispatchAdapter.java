package com.wisecan.unified.adapter.dispatch;

import com.wisecan.unified.domain.dispatch.external.ExternalDispatchPollingResult;
import com.wisecan.unified.domain.dispatch.external.ExternalSendRecord;
import com.wisecan.unified.domain.dispatch.external.ExternalSendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 외부 발송 시스템 어댑터 스텁 — 테스트·개발 전용 (W-204).
 *
 * <p>실제 중계사 DB 또는 HTTP API 연동 없이 외부 시스템 동작을 시뮬레이션한다.
 * 로컬 개발, 테스트 코드, CI 환경에서 사용한다.</p>
 *
 * <h3>동작 규약</h3>
 * <ul>
 *   <li>{@link #insert} — 항상 성공, 순번 증가 {@code msg_id} 를 반환한다.
 *       {@code msgType} 이 로그에 남으므로 채널 분기 검증이 가능하다.</li>
 *   <li>{@link #poll} — {@code msg_id} 를 받아 {@code messageState=3 / resultCode="0000"} 을
 *       즉시 반환한다 (finished 상태 시뮬레이션).</li>
 * </ul>
 *
 * <p>실제 중계사 구현체 교체 시 이 클래스를 제거하고
 * {@link ExternalDispatchAdapter} 구현체로 Bean 을 전환한다
 * (인터페이스 계약만 유지하면 서비스 코드 변경 없음).</p>
 */
@Component
@Slf4j
public class StubExternalDispatchAdapter implements ExternalDispatchAdapter {

    /** 스텁 msg_id 채번 카운터 (실제 외부 AUTO_INCREMENT 대체) */
    private final AtomicLong msgIdSequence = new AtomicLong(1_000_000L);

    /**
     * {@inheritDoc}
     *
     * <p>스텁 동작: 항상 성공. 채번된 {@code msgId} 반환.
     * 실제 환경에서는 외부 DB JDBC INSERT 또는 HTTP API 호출로 교체한다.</p>
     */
    @Override
    public ExternalSendResult insert(ExternalSendRecord record) {
        long assignedMsgId = msgIdSequence.getAndIncrement();

        log.info("[StubExternalDispatch] INSERT 시뮬레이션 — msgType={}, msgSubType={}, " +
                        "recipientCount={}, requestDate={}, assignedMsgId={}",
                record.msgType(),
                record.msgSubType(),
                record.recipientCount(),
                record.requestDate(),
                assignedMsgId);

        // etcChar1(=sendId), etcChar2(=routingMeta) 는 로그에 남기지 않는다 (INV-02).

        return ExternalSendResult.success(assignedMsgId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>스텁 동작: {@code messageState=3 (finished)} + {@code resultCode="0000"} 즉시 반환.
     * 실제 환경에서는 외부 DB SELECT 또는 HTTP 조회로 교체한다.</p>
     */
    @Override
    public ExternalDispatchPollingResult poll(Long msgId) {
        log.debug("[StubExternalDispatch] polling 시뮬레이션 — msgId={}", msgId);

        return new ExternalDispatchPollingResult(
                msgId,
                3,                          // messageState=3 (finished)
                "0000",                     // resultCode 성공
                LocalDateTime.now(),        // resultDeliverDate
                "KT"                        // resultNetId 예시
        );
    }
}
