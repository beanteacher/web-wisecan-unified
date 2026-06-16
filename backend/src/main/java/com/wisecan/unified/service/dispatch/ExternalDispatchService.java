package com.wisecan.unified.service.dispatch;

import com.wisecan.unified.adapter.dispatch.ExternalDispatchAdapter;
import com.wisecan.unified.domain.dispatch.SendRequest;
import com.wisecan.unified.domain.dispatch.external.ExternalSendRecord;
import com.wisecan.unified.domain.dispatch.external.ExternalSendResult;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.dispatch.SendRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 외부 발송 시스템 연동 서비스 (W-204).
 *
 * <p>책임 범위 — 05_DATA_MODEL §5.4 우리 책임 범위 2단계:</p>
 * <ol>
 *   <li>내부 {@link SendRequest} 엔티티(PENDING 상태)를 {@link ExternalSendRecord} 로 변환</li>
 *   <li>{@link ExternalDispatchAdapter#insert} 로 외부 시스템 INSERT 요청</li>
 *   <li>INSERT 성공 시 {@link SendRequest#markQueued(Long)} 로 상태 QUEUED + externalMsgId 업데이트</li>
 *   <li>INSERT 실패 시 {@link SendRequest#markFailed(String)} 로 상태 FAILED 기록 (보상 트랜잭션 대상)</li>
 * </ol>
 *
 * <p>외부 시스템 INSERT 이후의 상태 전이(message_state 1→2→3)와 결과 조회는
 * polling 으로 처리한다 — 현재 스프린트에서는 스텁 구현으로 검증,
 * 이후 스프린트에서 실제 중계사 연동 구현체로 교체한다.</p>
 *
 * <p>routing_meta(중계사 매핑) 는 {@link ExternalSendRecord#etcChar2()} 에 담기며
 * 회원·API 응답에 절대 노출 금지 (INV-02).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalDispatchService {

    private final ExternalDispatchAdapter externalDispatchAdapter;
    private final SendRequestRepository sendRequestRepository;

    /**
     * PENDING 상태의 발송 요청을 외부 시스템에 INSERT하고 상태를 갱신한다.
     *
     * <p>외부 INSERT 성공 → {@link SendRequest#markQueued(Long)} (status=QUEUED).<br>
     * 외부 INSERT 실패 → {@link SendRequest#markFailed(String)} (status=FAILED).
     * 실패 시 잔액 보상 트랜잭션은 호출자(W-205 이후 구현)가 처리한다.</p>
     *
     * @param sendId     내부 발송 요청 ULID (26자)
     * @param routingMeta 중계사 라우팅 메타 (etc_char_2 에 저장, 회원 비노출)
     * @return 외부 시스템 INSERT 결과
     */
    @Transactional(rollbackFor = Exception.class)
    public ExternalSendResult dispatch(String sendId, String routingMeta) {
        SendRequest req = sendRequestRepository.findBySendId(sendId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "발송 요청을 찾을 수 없습니다: sendId=" + sendId));

        ExternalSendRecord record = ExternalSendRecord.from(req, routingMeta, null);

        ExternalSendResult result = externalDispatchAdapter.insert(record);

        if (result.success()) {
            req.markQueued(result.msgId());
            log.info("[ExternalDispatch] 외부 INSERT 완료 — sendId={}, externalMsgId={}",
                    sendId, result.msgId());
        } else {
            req.markFailed(result.errorMessage());
            log.warn("[ExternalDispatch] 외부 INSERT 실패 — sendId={}, reason={}",
                    sendId, result.errorMessage());
        }

        return result;
    }

    /**
     * 외부 시스템에서 발송 진행 상태를 polling 조회한다.
     *
     * <p>조회 대상: {@code send_*_tran} 우선, 없으면 {@code send_*_log_YYYYMM} 탐색.
     * 호출 주기는 호출자가 관리한다 (테스트망 10초, 상용망 60초 — 합의 규약).</p>
     *
     * @param externalMsgId 외부 시스템 메시지 ID ({@link ExternalSendResult#msgId()})
     * @return polling 결과
     */
    public com.wisecan.unified.domain.dispatch.external.ExternalDispatchPollingResult poll(Long externalMsgId) {
        return externalDispatchAdapter.poll(externalMsgId);
    }
}
