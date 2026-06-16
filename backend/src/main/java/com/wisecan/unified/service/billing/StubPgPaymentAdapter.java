package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.PaymentMethodType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * PG 결제 Stub 구현체 — MVP/개발 환경 전용.
 * application.yml: pg.stub=true 일 때만 Bean 등록.
 * 실 PG 연동 시 이 빈을 비활성화하고 실 구현체를 등록한다.
 *
 * 동작: 항상 SUCCESS 반환 + pgTxId = "STUB-{UUID}".
 */
@Component
@ConditionalOnProperty(name = "pg.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubPgPaymentAdapter implements PgPaymentPort {

    @Override
    public PgPaymentResult requestPayment(Long memberId, PaymentMethodType methodType,
                                          String pgBillingKey, long amount, String idempotencyKey) {
        String stubTxId = "STUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.info("[StubPG] 결제 요청 — memberId={} methodType={} amount={} idempotencyKey={} → pgTxId={}",
                memberId, methodType, amount, idempotencyKey, stubTxId);
        return PgPaymentResult.success(stubTxId);
    }

    @Override
    public boolean cancelPayment(String pgTxId, long amount) {
        log.info("[StubPG] 결제 취소 — pgTxId={} amount={}", pgTxId, amount);
        return true;
    }
}
