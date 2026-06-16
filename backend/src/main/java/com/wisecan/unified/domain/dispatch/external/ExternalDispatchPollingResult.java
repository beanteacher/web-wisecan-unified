package com.wisecan.unified.domain.dispatch.external;

import java.time.LocalDateTime;

/**
 * 외부 발송 시스템 polling 조회 결과 — 합의 스펙 (W-204).
 *
 * <p>외부 {@code send_*_tran} 또는 {@code send_*_log_YYYYMM} 테이블을
 * SELECT 해서 얻은 발송 진행 상태를 표현한다. 05_DATA_MODEL §5.2·§5.3 기준.</p>
 *
 * <h3>polling 규약 (합의 요약)</h3>
 * <ul>
 *   <li>조회 대상: {@code send_*_tran} (진행 중) + {@code send_*_log_YYYYMM} (완료·이관)</li>
 *   <li>조회 키: {@code msg_id} (외부 시스템 발급, {@link ExternalSendResult#msgId()} 로 수신)</li>
 *   <li>조회 주기: 최초 30초 후 → 이후 60초 간격 (상용 환경 기준; 테스트망 10초)</li>
 *   <li>{@code message_state} 3 또는 4 도달 시 polling 종료</li>
 *   <li>결과 코드({@code resultCode}) 는 외부 시스템 정의 통신사 결과 코드 그대로 보존</li>
 *   <li>회원 이력 화면용 상태 변환은 {@code messageState} 값 기준으로 내부 매핑</li>
 * </ul>
 *
 * <h3>message_state 카탈로그 (§5.3)</h3>
 * <ul>
 *   <li>0 init      — 입력 초기 (본 서비스 INSERT 직후)</li>
 *   <li>1 fetched   — 외부 시스템 송출 큐로 가져감</li>
 *   <li>2 submitted — 외부 시스템 송출 성공</li>
 *   <li>3 finished  — 처리 완료 (_log_YYYYMM 이관 대상)</li>
 *   <li>4 logfail   — 로그 이관 실패 (외부 운영자 점검 대상)</li>
 * </ul>
 */
public record ExternalDispatchPollingResult(

        /** 외부 시스템 메시지 ID */
        Long msgId,

        /**
         * 메시지 상태 코드 (0~4). §5.3 카탈로그.
         * 3(finished) 또는 4(logfail) 이면 polling 종료 조건.
         */
        int messageState,

        /**
         * 통신사 결과 코드.
         * 외부 시스템이 {@code result_code} 컬럼에 기록한 값 그대로.
         * 성공: "0000" / 그 외: 통신사 정의 에러 코드.
         * messageState < 2 이면 null (아직 미도달).
         */
        String resultCode,

        /**
         * 최종 수신 일시 ({@code result_deliver_date}).
         * messageState 3 이후 기록; 그 이전은 null.
         */
        LocalDateTime resultDeliverDate,

        /**
         * 망 식별자 ({@code result_net_id}).
         * 통신사 구분 코드; 결과 확인 후 기록 (옵션, null 가능).
         */
        String resultNetId

) {

    /** polling 종료 조건: messageState 가 3(finished) 또는 4(logfail) */
    public boolean isTerminal() {
        return messageState == 3 || messageState == 4;
    }

    /** 발송 성공 여부: messageState=3 + resultCode="0000" */
    public boolean isDelivered() {
        return messageState == 3 && "0000".equals(resultCode);
    }
}
