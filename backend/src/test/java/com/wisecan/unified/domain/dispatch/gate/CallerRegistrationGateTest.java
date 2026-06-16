package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallerRegistrationGate — 발신번호 등록 여부 검증")
class CallerRegistrationGateTest {

    @InjectMocks
    private CallerRegistrationGate gate;

    @Mock
    private CallbackRepository callbackRepository;

    private SendValidationContext ctx(String callbackNumber) {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, callbackNumber,
                SendChannel.SMS, "안녕하세요", false, 1, 10L, NetworkType.TEST, null);
    }

    @Test
    @DisplayName("REGISTERED 발신번호 — 통과")
    void registered_passes() {
        given(callbackRepository.existsByMemberIdAndPhoneNumberAndStatus(
                1L, "01012345678", CallbackStatus.REGISTERED)).willReturn(true);

        assertThatCode(() -> gate.validate(ctx("01012345678"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("미등록 발신번호 — SendValidationException(CALLER_NOT_REGISTERED)")
    void notRegistered_throws() {
        given(callbackRepository.existsByMemberIdAndPhoneNumberAndStatus(
                1L, "01099999999", CallbackStatus.REGISTERED)).willReturn(false);

        assertThatThrownBy(() -> gate.validate(ctx("01099999999")))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex -> {
                    SendValidationException sve = (SendValidationException) ex;
                    org.assertj.core.api.Assertions.assertThat(
                            sve.getErrorCode().name()).isEqualTo("CALLER_NOT_REGISTERED");
                });
    }

    @Test
    @DisplayName("order() == 10")
    void order_is_10() {
        org.assertj.core.api.Assertions.assertThat(gate.order()).isEqualTo(10);
    }
}
