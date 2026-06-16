package com.wisecan.unified.service.billing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 자동충전 실패 알림 리스너 — W-402.
 *
 * AutoChargeFailedEvent 를 AFTER_COMMIT 단계에서 수신.
 * MVP: 로그만 남김 (실 알림 연동은 별도 알림 모듈에서 구독).
 * 실 구현 시 이 리스너에서 SMS/이메일/알림톡 발송 서비스를 주입해 호출한다.
 */
@Component
@Slf4j
public class AutoChargeFailedListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAutoChargeFailed(AutoChargeFailedEvent event) {
        log.warn("[자동충전 실패 알림] memberId={} configId={} 시도금액={}원 사유={}",
                event.memberId(), event.configId(),
                event.attemptedAmount(), event.failReason());
        // TODO: 알림 모듈 연동 — 회원에게 SMS/이메일, 운영자에게 어드민 알림 발송
    }
}
