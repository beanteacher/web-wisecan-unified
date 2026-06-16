package com.wisecan.unified.domain.dispatch;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.gate.AdDisclosureGate;
import com.wisecan.unified.domain.dispatch.gate.BalanceGate;
import com.wisecan.unified.domain.dispatch.gate.CallerRegistrationGate;
import com.wisecan.unified.domain.dispatch.gate.CallerWhitelistGate;
import com.wisecan.unified.domain.dispatch.gate.DailyLimitGate;
import com.wisecan.unified.domain.dispatch.gate.NetworkRoutingGate;
import com.wisecan.unified.domain.dispatch.gate.NightAdBlockGate;
import com.wisecan.unified.domain.dispatch.gate.PostpaidBlockGate;
import com.wisecan.unified.domain.dispatch.gate.ScopePermissionGate;
import com.wisecan.unified.domain.dispatch.gate.SpamKeywordGate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;

/**
 * SendValidationService -- 오케스트레이터 통합 케이스.
 * 10종 게이트를 order() 오름차순으로 실행하고 첫 실패에서 즉시 종료하는 동작을 검증한다.
 * W-205: NetworkRoutingGate(order=5)가 파이프라인 최전방에 추가됨.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendValidationService -- 10종 게이트 파이프라인 오케스트레이터")
class SendValidationServiceTest {

    @Mock private NetworkRoutingGate      networkRoutingGate;       // order  5 (W-205)
    @Mock private CallerRegistrationGate  callerRegistrationGate;   // order 10
    @Mock private ScopePermissionGate     scopePermissionGate;       // order 20
    @Mock private CallerWhitelistGate     callerWhitelistGate;       // order 30
    @Mock private DailyLimitGate          dailyLimitGate;            // order 40
    @Mock private SpamKeywordGate         spamKeywordGate;            // order 50
    @Mock private AdDisclosureGate        adDisclosureGate;           // order 60
    @Mock private NightAdBlockGate        nightAdBlockGate;           // order 70
    @Mock private BalanceGate             balanceGate;                // order 80
    @Mock private PostpaidBlockGate       postpaidBlockGate;          // order 90

    private SendValidationService buildService() {
        given(networkRoutingGate.order()).willReturn(5);
        given(callerRegistrationGate.order()).willReturn(10);
        given(scopePermissionGate.order()).willReturn(20);
        given(callerWhitelistGate.order()).willReturn(30);
        given(dailyLimitGate.order()).willReturn(40);
        given(spamKeywordGate.order()).willReturn(50);
        given(adDisclosureGate.order()).willReturn(60);
        given(nightAdBlockGate.order()).willReturn(70);
        given(balanceGate.order()).willReturn(80);
        given(postpaidBlockGate.order()).willReturn(90);

        return new SendValidationService(List.of(
                networkRoutingGate,
                callerRegistrationGate,
                scopePermissionGate,
                callerWhitelistGate,
                dailyLimitGate,
                spamKeywordGate,
                adDisclosureGate,
                nightAdBlockGate,
                balanceGate,
                postpaidBlockGate
        ));
    }

    private SendValidationContext ctx() {
        return new SendValidationContext(1L, 10L, ApiKeyType.TEST, "01012345678",
                SendChannel.SMS, "안녕하세요", false, 1, 10L, NetworkType.TEST);
    }

    @Test
    @DisplayName("10종 게이트 모두 통과 -- 예외 없음")
    void allGatesPass_noException() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        assertThatCode(() -> service.validate(context)).doesNotThrowAnyException();

        verify(networkRoutingGate).validate(context);
        verify(callerRegistrationGate).validate(context);
        verify(scopePermissionGate).validate(context);
        verify(callerWhitelistGate).validate(context);
        verify(dailyLimitGate).validate(context);
        verify(spamKeywordGate).validate(context);
        verify(adDisclosureGate).validate(context);
        verify(nightAdBlockGate).validate(context);
        verify(balanceGate).validate(context);
        verify(postpaidBlockGate).validate(context);
    }

    @Test
    @DisplayName("W-205: NetworkRoutingGate(order=5) — 테스트 키로 상용망 시도 시 즉시 차단")
    void networkRoutingGateFails_stopsBeforeAllOtherGates() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.TEST_KEY_PRODUCTION_ROUTE_DENIED))
                .when(networkRoutingGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.TEST_KEY_PRODUCTION_ROUTE_DENIED));

        verify(callerRegistrationGate, never()).validate(context);
        verify(scopePermissionGate, never()).validate(context);
        verify(balanceGate, never()).validate(context);
        verify(postpaidBlockGate, never()).validate(context);
    }

    @Test
    @DisplayName("첫 번째 게이트(CallerRegistration, order=10) 실패 -- 즉시 중단, 나머지 미실행")
    void firstGateFails_stopsImmediately() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.CALLER_NOT_REGISTERED))
                .when(callerRegistrationGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.CALLER_NOT_REGISTERED));

        verify(networkRoutingGate).validate(context);
        verify(scopePermissionGate, never()).validate(context);
        verify(balanceGate, never()).validate(context);
        verify(postpaidBlockGate, never()).validate(context);
    }

    @Test
    @DisplayName("중간 게이트(SpamKeyword, order=50) 실패 -- 이전 5종 통과, 이후 미실행")
    void middleGateFails_previousPassedNextSkipped() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.SPAM_KEYWORD_DETECTED))
                .when(spamKeywordGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SPAM_KEYWORD_DETECTED));

        verify(networkRoutingGate).validate(context);
        verify(callerRegistrationGate).validate(context);
        verify(scopePermissionGate).validate(context);
        verify(callerWhitelistGate).validate(context);
        verify(dailyLimitGate).validate(context);
        verify(adDisclosureGate, never()).validate(context);
        verify(balanceGate, never()).validate(context);
        verify(postpaidBlockGate, never()).validate(context);
    }

    @Test
    @DisplayName("마지막 게이트(PostpaidBlock, order=90) 실패 -- 앞 9종 통과")
    void lastGateFails_allPreviousPassed() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.INSUFFICIENT_BALANCE,
                "미납 청구서가 있어 발송이 차단되었습니다."))
                .when(postpaidBlockGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE));

        verify(networkRoutingGate).validate(context);
        verify(callerRegistrationGate).validate(context);
        verify(scopePermissionGate).validate(context);
        verify(callerWhitelistGate).validate(context);
        verify(dailyLimitGate).validate(context);
        verify(spamKeywordGate).validate(context);
        verify(adDisclosureGate).validate(context);
        verify(nightAdBlockGate).validate(context);
        verify(balanceGate).validate(context);
    }

    @Test
    @DisplayName("잔액 게이트(Balance, order=80) 실패 -- INSUFFICIENT_BALANCE, 상세 메시지 포함")
    void balanceGateFails_insufficientBalanceWithMessage() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.INSUFFICIENT_BALANCE,
                "잔액이 부족합니다. 현재 잔액: 0원, 필요 금액: 10원."))
                .when(balanceGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex -> {
                    SendValidationException sve = (SendValidationException) ex;
                    assertThat(sve.getErrorCode()).isEqualTo(SendErrorCode.INSUFFICIENT_BALANCE);
                    assertThat(sve.getMessage()).contains("잔액이 부족합니다");
                });

        verify(postpaidBlockGate, never()).validate(context);
    }

    @Test
    @DisplayName("게이트 역순 등록 -- order() 오름차순 정렬 실행 확인")
    void gatesExecutedInOrderRegardlessOfRegistrationOrder() {
        given(networkRoutingGate.order()).willReturn(5);
        given(callerRegistrationGate.order()).willReturn(10);
        given(scopePermissionGate.order()).willReturn(20);
        given(callerWhitelistGate.order()).willReturn(30);
        given(dailyLimitGate.order()).willReturn(40);
        given(spamKeywordGate.order()).willReturn(50);
        given(adDisclosureGate.order()).willReturn(60);
        given(nightAdBlockGate.order()).willReturn(70);
        given(balanceGate.order()).willReturn(80);
        given(postpaidBlockGate.order()).willReturn(90);

        SendValidationService service = new SendValidationService(List.of(
                postpaidBlockGate,
                balanceGate,
                nightAdBlockGate,
                adDisclosureGate,
                spamKeywordGate,
                dailyLimitGate,
                callerWhitelistGate,
                scopePermissionGate,
                callerRegistrationGate,
                networkRoutingGate
        ));

        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.CALLER_NOT_REGISTERED))
                .when(callerRegistrationGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.CALLER_NOT_REGISTERED));

        verify(networkRoutingGate).validate(context);
        verify(postpaidBlockGate, never()).validate(context);
        verify(balanceGate, never()).validate(context);
    }

    @Test
    @DisplayName("스코프 부족 게이트(ScopePermission, order=20) 실패 -- SCOPE_NOT_GRANTED")
    void scopeGateFails_scopeNotGranted() {
        SendValidationService service = buildService();
        SendValidationContext context = ctx();

        doThrow(new SendValidationException(SendErrorCode.SCOPE_NOT_GRANTED))
                .when(scopePermissionGate).validate(context);

        assertThatThrownBy(() -> service.validate(context))
                .isInstanceOf(SendValidationException.class)
                .satisfies(ex ->
                        assertThat(((SendValidationException) ex).getErrorCode())
                                .isEqualTo(SendErrorCode.SCOPE_NOT_GRANTED));

        verify(networkRoutingGate).validate(context);
        verify(callerRegistrationGate).validate(context);
        verify(callerWhitelistGate, never()).validate(context);
    }
}
