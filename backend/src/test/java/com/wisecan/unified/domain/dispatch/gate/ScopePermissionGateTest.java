package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.repository.ApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScopePermissionGate — API Key 스코프 권한 검증")
class ScopePermissionGateTest {

    @InjectMocks
    private ScopePermissionGate gate;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private SendValidationContext ctx(SendChannel channel) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                channel, "안녕하세요", false, 1, 10L, NetworkType.TEST);
    }

    private ApiKey activeKey(Set<ApiKeyScope> scopes) {
        return ApiKey.builder()
                .keyName("테스트키").keyPrefix("wc_test").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.TEST)
                .scopes(scopes).build();
    }

    @Test
    @DisplayName("SEND(전채널) 스코프 보유 — SMS 채널 통과")
    void sendAllScope_passes() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(activeKey(Set.of(ApiKeyScope.SEND))));

        assertThatCode(() -> gate.validate(ctx(SendChannel.SMS))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEND_SMS 스코프 보유 — SMS 채널 통과")
    void sendSmsScope_passesForSms() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(activeKey(Set.of(ApiKeyScope.SEND_SMS))));

        assertThatCode(() -> gate.validate(ctx(SendChannel.SMS))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SEND_KAKAO 스코프 보유 — 카카오 채널 통과")
    void sendKakaoScope_passesForKakao() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(activeKey(Set.of(ApiKeyScope.SEND_KAKAO))));

        assertThatCode(() -> gate.validate(ctx(SendChannel.KAKAO))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("스코프 없음 — SCOPE_NOT_GRANTED")
    void noScope_throws() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(activeKey(Set.of(ApiKeyScope.HISTORY_READ))));

        assertThatThrownBy(() -> gate.validate(ctx(SendChannel.SMS)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SCOPE_NOT_GRANTED));
    }

    @Test
    @DisplayName("REVOKED 키 — API_KEY_REVOKED")
    void revokedKey_throws() {
        ApiKey revoked = ApiKey.builder()
                .keyName("폐기키").keyPrefix("wc_rev").keyHash("hash")
                .status(ApiKeyStatus.REVOKED).keyType(ApiKeyType.TEST)
                .scopes(Set.of(ApiKeyScope.SEND)).build();
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> gate.validate(ctx(SendChannel.SMS)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.API_KEY_REVOKED));
    }

    @Test
    @DisplayName("PENDING_REVIEW 키 — API_KEY_NOT_ACTIVE")
    void pendingKey_throws() {
        ApiKey pending = ApiKey.builder()
                .keyName("심사중키").keyPrefix("wc_pen").keyHash("hash")
                .status(ApiKeyStatus.PENDING_REVIEW).keyType(ApiKeyType.PRODUCTION)
                .scopes(Set.of(ApiKeyScope.SEND)).build();
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> gate.validate(ctx(SendChannel.SMS)))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.API_KEY_NOT_ACTIVE));
    }

    @Test
    @DisplayName("order() == 20")
    void order_is_20() {
        assertThat(gate.order()).isEqualTo(20);
    }
}
