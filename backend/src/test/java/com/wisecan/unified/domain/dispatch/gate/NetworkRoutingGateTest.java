package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NetworkRoutingGate — 망 분리 라우팅 검증 (W-205 DoD).
 *
 * <p>핵심 DoD: 테스트 키로 상용망 발송 시도 시 명시적 거부 응답 반환.</p>
 */
@DisplayName("NetworkRoutingGate — 테스트/상용 망 분리 검증 (W-205)")
class NetworkRoutingGateTest {

    private final NetworkRoutingGate gate = new NetworkRoutingGate();

    private SendValidationContext ctx(ApiKeyType keyType, NetworkType networkType) {
        return new SendValidationContext(1L, 10L, keyType, "01012345678",
                SendChannel.SMS, "안녕하세요", false, 1, 10L, networkType, null);
    }

    // ── DoD 핵심: 테스트 키 → 상용망 차단 ─────────────────────────────

    @Test
    @DisplayName("【W-205 DoD】 테스트 키 + 상용망 요청 → TEST_KEY_PRODUCTION_ROUTE_DENIED")
    void testKey_productionNetwork_throws() {
        assertThatThrownBy(() -> gate.validate(ctx(ApiKeyType.TEST, NetworkType.PRODUCTION)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex -> {
                    SendValidationException sve = (SendValidationException) ex;
                    assertThat(sve.getErrorCode())
                            .isEqualTo(SendErrorCode.TEST_KEY_PRODUCTION_ROUTE_DENIED);
                    assertThat(sve.getMessage())
                            .contains("테스트 키")
                            .contains("상용망");
                });
    }

    // ── 상용 키 → 테스트망 차단 ──────────────────────────────────────

    @Test
    @DisplayName("상용 키 + 테스트망 요청 → PRODUCTION_KEY_TEST_ROUTE_DENIED")
    void productionKey_testNetwork_throws() {
        assertThatThrownBy(() -> gate.validate(ctx(ApiKeyType.PRODUCTION, NetworkType.TEST)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex -> {
                    SendValidationException sve = (SendValidationException) ex;
                    assertThat(sve.getErrorCode())
                            .isEqualTo(SendErrorCode.PRODUCTION_KEY_TEST_ROUTE_DENIED);
                    assertThat(sve.getMessage())
                            .contains("상용 키")
                            .contains("테스트망");
                });
    }

    // ── 정상 조합: 통과 ───────────────────────────────────────────────

    @Test
    @DisplayName("테스트 키 + 테스트망 — 통과")
    void testKey_testNetwork_passes() {
        assertThatCode(() -> gate.validate(ctx(ApiKeyType.TEST, NetworkType.TEST)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("상용 키 + 상용망 — 통과")
    void productionKey_productionNetwork_passes() {
        assertThatCode(() -> gate.validate(ctx(ApiKeyType.PRODUCTION, NetworkType.PRODUCTION)))
                .doesNotThrowAnyException();
    }

    // ── 에러 메시지 상세 검증 ─────────────────────────────────────────

    @Test
    @DisplayName("TEST_KEY_PRODUCTION_ROUTE_DENIED — 에러 메시지에 apiKeyId 포함")
    void testKey_productionNetwork_messageContainsApiKeyId() {
        SendValidationContext ctxWithKeyId = new SendValidationContext(
                5L, 42L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "메시지", false, 1, 10L, NetworkType.PRODUCTION, null);

        assertThatThrownBy(() -> gate.validate(ctxWithKeyId))
                .isInstanceOf(SendValidationException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("PRODUCTION_KEY_TEST_ROUTE_DENIED — 에러 메시지에 apiKeyId 포함")
    void productionKey_testNetwork_messageContainsApiKeyId() {
        SendValidationContext ctxWithKeyId = new SendValidationContext(
                5L, 99L, ApiKeyType.PRODUCTION, "01012345678",
                SendChannel.SMS, "메시지", false, 1, 10L, NetworkType.TEST, null);

        assertThatThrownBy(() -> gate.validate(ctxWithKeyId))
                .isInstanceOf(SendValidationException.class)
                .hasMessageContaining("99");
    }

    // ── 에러 코드 기본 메시지 검증 ────────────────────────────────────

    @Test
    @DisplayName("TEST_KEY_PRODUCTION_ROUTE_DENIED 에러 코드 기본 메시지 확인")
    void errorCode_testKeyProductionDenied_defaultMessage() {
        assertThat(SendErrorCode.TEST_KEY_PRODUCTION_ROUTE_DENIED.getDefaultMessage())
                .isEqualTo("테스트 키로는 상용망 발송을 요청할 수 없습니다.");
    }

    @Test
    @DisplayName("PRODUCTION_KEY_TEST_ROUTE_DENIED 에러 코드 기본 메시지 확인")
    void errorCode_productionKeyTestDenied_defaultMessage() {
        assertThat(SendErrorCode.PRODUCTION_KEY_TEST_ROUTE_DENIED.getDefaultMessage())
                .isEqualTo("상용 키로는 테스트망 발송을 요청할 수 없습니다.");
    }

    // ── order 검증 ────────────────────────────────────────────────────

    @Test
    @DisplayName("order() == 5 — 모든 게이트 중 최우선 실행")
    void order_is_5() {
        assertThat(gate.order()).isEqualTo(5);
    }
}
