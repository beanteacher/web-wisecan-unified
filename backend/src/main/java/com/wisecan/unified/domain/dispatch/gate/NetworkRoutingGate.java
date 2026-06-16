package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import org.springframework.stereotype.Component;

/**
 * 검증 0 — 테스트망/상용망 라우팅 분리 게이트 (W-205).
 *
 * <p>API Key 유형({@link ApiKeyType})과 요청된 발송 망({@link NetworkType})이
 * 일치하는지 검증한다. 불일치 시 즉시 거부한다.</p>
 *
 * <ul>
 *   <li>TEST 키 + PRODUCTION 망 요청 → 거부 ({@link SendErrorCode#TEST_KEY_PRODUCTION_ROUTE_DENIED})</li>
 *   <li>PRODUCTION 키 + TEST 망 요청 → 거부 ({@link SendErrorCode#PRODUCTION_KEY_TEST_ROUTE_DENIED})</li>
 *   <li>키 유형 = 망 유형 → 통과</li>
 * </ul>
 *
 * <p>order=5 — CallerRegistrationGate(10)보다 먼저 실행되어
 * 망 분리 위반을 가장 이른 시점에 차단한다.</p>
 */
@Component
public class NetworkRoutingGate implements SendValidationGate {

    @Override
    public void validate(SendValidationContext ctx) {
        ApiKeyType keyType     = ctx.apiKeyType();
        NetworkType networkType = ctx.networkType();

        if (keyType == ApiKeyType.TEST && networkType == NetworkType.PRODUCTION) {
            throw new SendValidationException(
                    SendErrorCode.TEST_KEY_PRODUCTION_ROUTE_DENIED,
                    "테스트 키(apiKeyId=" + ctx.apiKeyId() + ")로 상용망 발송을 시도했습니다. 상용 키를 사용하세요."
            );
        }

        if (keyType == ApiKeyType.PRODUCTION && networkType == NetworkType.TEST) {
            throw new SendValidationException(
                    SendErrorCode.PRODUCTION_KEY_TEST_ROUTE_DENIED,
                    "상용 키(apiKeyId=" + ctx.apiKeyId() + ")로 테스트망 발송을 시도했습니다. 테스트 키를 사용하세요."
            );
        }
    }

    @Override
    public int order() {
        return 5;
    }
}
