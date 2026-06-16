package com.wisecan.unified.adapter.dispatch;

import com.wisecan.unified.domain.dispatch.external.ExternalDispatchPollingResult;
import com.wisecan.unified.domain.dispatch.external.ExternalSendRecord;
import com.wisecan.unified.domain.dispatch.external.ExternalSendResult;

/**
 * 외부 발송 시스템(중계사) 연동 어댑터 인터페이스 — W-204 합의 스펙.
 *
 * <p>본 서비스와 외부 발송 시스템 사이의 I/O 경계를 격리한다 (Adapter 패턴).
 * 컨트롤러·서비스는 이 인터페이스에만 의존하고,
 * 실제 중계사 연동(DB 직접 INSERT 또는 HTTP API 호출)은 구현체에서 처리한다.</p>
 *
 * <h3>현재 구현체</h3>
 * <ul>
 *   <li>{@link StubExternalDispatchAdapter} — 테스트·개발용 스텁 (W-204)</li>
 *   <li>실제 중계사 구현체 — W-204 이후 스프린트에서 추가 예정</li>
 * </ul>
 *
 * <h3>인터페이스 계약 (합의 규약 요약)</h3>
 * <ul>
 *   <li>{@link #insert} — {@code send_*_tran} 테이블에 1행 INSERT 요청.
 *       성공 시 외부 시스템 발급 {@code msg_id} 를 {@link ExternalSendResult} 로 반환.</li>
 *   <li>{@link #poll} — {@code msg_id} 로 {@code send_*_tran} 또는
 *       {@code send_*_log_YYYYMM} 에서 진행 상태 SELECT.
 *       {@link ExternalDispatchPollingResult#isTerminal()} 이 {@code true} 면 polling 종료.</li>
 * </ul>
 *
 * <p>routing_meta(중계사 매핑) 는 {@link ExternalSendRecord#etcChar2()} 에 담겨 전달되며
 * 회원·API 응답에 절대 노출 금지 (INV-02, 05_DATA_MODEL §5.5).</p>
 */
public interface ExternalDispatchAdapter {

    /**
     * 외부 발송 시스템 {@code send_*_tran} 테이블에 발송 행을 INSERT한다.
     *
     * <p>채널 분기:
     * <ul>
     *   <li>SMS/LMS/MMS → {@code send_sms_tran}</li>
     *   <li>MMS(첨부) → {@code send_mms_tran}</li>
     *   <li>KAKAO → {@code send_kko_tran}</li>
     *   <li>RCS → {@code send_rcs_tran}</li>
     * </ul>
     * 분기 기준은 {@link ExternalSendRecord#msgType()} 값이다.</p>
     *
     * @param record 외부 INSERT 페이로드 ({@link ExternalSendRecord#from()} 으로 생성)
     * @return INSERT 결과 — 성공 시 {@code msgId} non-null, 실패 시 {@code success=false}
     */
    ExternalSendResult insert(ExternalSendRecord record);

    /**
     * 외부 발송 시스템에서 {@code msg_id} 기준으로 발송 진행 상태를 조회한다.
     *
     * <p>조회 순서: {@code send_*_tran} 우선 조회 → 없으면 당월 {@code send_*_log_YYYYMM}
     * → 이전 월 순으로 탐색 (05_DATA_MODEL §5.1 진행·로그 테이블 패밀리).</p>
     *
     * <p>polling 주기 정책 (합의):
     * <ul>
     *   <li>테스트망: 10초 간격</li>
     *   <li>상용망: 최초 30초 후 → 이후 60초 간격</li>
     * </ul>
     * 주기 제어는 호출자({@code ExternalDispatchService})가 담당한다.</p>
     *
     * @param msgId 외부 시스템 메시지 ID ({@link ExternalSendResult#msgId()} 로 수신한 값)
     * @return polling 결과; 아직 조회 불가 상태면 messageState=0 을 담은 결과 반환
     */
    ExternalDispatchPollingResult poll(Long msgId);
}
