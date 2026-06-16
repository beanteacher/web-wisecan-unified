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
@DisplayName("CallerWhitelistGate — 발신번호 화이트리스트 검증")
class CallerWhitelistGateTest {

    @InjectMocks
    private CallerWhitelistGate gate;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private SendValidationContext ctx(String callbackNumber) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, callbackNumber,
                SendChannel.SMS, "안녕하세요", false, 1, 10L, NetworkType.TEST);
    }

    private ApiKey keyWithWhitelist(String allowedRaw) {
        return ApiKey.builder()
                .keyName("테스트키").keyPrefix("wc_test").keyHash("hash")
                .status(ApiKeyStatus.ACTIVE).keyType(ApiKeyType.TEST)
                .scopes(Set.of(ApiKeyScope.SEND_SMS))
                .allowedCallbacksRaw(allowedRaw).build();
    }

    @Test
    @DisplayName("화이트리스트 미설정(null) — 모든 번호 통과")
    void noWhitelist_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithWhitelist(null)));

        assertThatCode(() -> gate.validate(ctx("01012345678"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("화이트리스트 미설정(blank) — 모든 번호 통과")
    void blankWhitelist_passes() {
        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(keyWithWhitelist("  ")));

        assertThatCode(() -> gate.validate(ctx("01012345678"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("화이트리스트에 포함된 번호 — 통과")
    void numberInWhitelist_passes() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(keyWithWhitelist("010-1234-5678,010-9999-9999")));

        assertThatCode(() -> gate.validate(ctx("01012345678"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("화이트리스트에 없는 번호 — CALLER_NOT_IN_WHITELIST")
    void numberNotInWhitelist_throws() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(keyWithWhitelist("01012345678")));

        assertThatThrownBy(() -> gate.validate(ctx("01099999999")))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.CALLER_NOT_IN_WHITELIST));
    }

    @Test
    @DisplayName("하이픈 포함 화이트리스트 항목과 숫자만 입력 — 정규화 후 통과")
    void hyphenNormalization_passes() {
        given(apiKeyRepository.findById(10L))
                .willReturn(Optional.of(keyWithWhitelist("010-1234-5678")));

        assertThatCode(() -> gate.validate(ctx("01012345678"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("order() == 30")
    void order_is_30() {
        assertThat(gate.order()).isEqualTo(30);
    }
}
