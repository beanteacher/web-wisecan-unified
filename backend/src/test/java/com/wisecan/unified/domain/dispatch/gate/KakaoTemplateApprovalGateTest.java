package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.service.template.KakaoTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoTemplateApprovalGate 단위 테스트")
class KakaoTemplateApprovalGateTest {

    @Mock
    private KakaoTemplateService kakaoTemplateService;

    @InjectMocks
    private KakaoTemplateApprovalGate gate;

    private SendValidationContext ctx(SendChannel channel, String templateCode) {
        return new SendValidationContext(
                1L, 10L, ApiKeyType.TEST, "01012345678",
                channel, "알림 메시지", false, 1, 10L, NetworkType.TEST, templateCode);
    }

    // ── SMS — 게이트 스킵 ─────────────────────────────────────────

    @Nested
    @DisplayName("비카카오 채널")
    class NonKakaoChannelTest {

        @Test
        @DisplayName("SMS 채널 → 검증 없이 통과")
        void sms_skipsValidation() {
            assertThatCode(() -> gate.validate(ctx(SendChannel.SMS, null)))
                    .doesNotThrowAnyException();
            verify(kakaoTemplateService, never()).isApproved(1L, null);
        }

        @Test
        @DisplayName("RCS 채널 → 검증 없이 통과")
        void rcs_skipsValidation() {
            assertThatCode(() -> gate.validate(ctx(SendChannel.RCS, "rcs_tmpl")))
                    .doesNotThrowAnyException();
        }
    }

    // ── KAKAO 채널 ────────────────────────────────────────────────

    @Nested
    @DisplayName("카카오 채널")
    class KakaoChannelTest {

        @Test
        @DisplayName("templateCode 없음 → TEMPLATE_REQUIRED")
        void kakao_noTemplateCode_throws() {
            assertThatThrownBy(() -> gate.validate(ctx(SendChannel.KAKAO, null)))
                    .isInstanceOf(SendValidationException.class)
                    .satisfies(ex -> {
                        SendValidationException sve = (SendValidationException) ex;
                        assertThat(sve.getErrorCode()).isEqualTo(SendErrorCode.TEMPLATE_REQUIRED);
                    });
        }

        @Test
        @DisplayName("templateCode 공백 → TEMPLATE_REQUIRED")
        void kakao_blankTemplateCode_throws() {
            assertThatThrownBy(() -> gate.validate(ctx(SendChannel.KAKAO, "  ")))
                    .isInstanceOf(SendValidationException.class)
                    .satisfies(ex -> assertThat(((SendValidationException) ex).getErrorCode())
                            .isEqualTo(SendErrorCode.TEMPLATE_REQUIRED));
        }

        @Test
        @DisplayName("미승인 템플릿 → TEMPLATE_NOT_APPROVED")
        void kakao_notApproved_throws() {
            given(kakaoTemplateService.isApproved(1L, "tmpl_001")).willReturn(false);

            assertThatThrownBy(() -> gate.validate(ctx(SendChannel.KAKAO, "tmpl_001")))
                    .isInstanceOf(SendValidationException.class)
                    .satisfies(ex -> {
                        SendValidationException sve = (SendValidationException) ex;
                        assertThat(sve.getErrorCode()).isEqualTo(SendErrorCode.TEMPLATE_NOT_APPROVED);
                        assertThat(sve.getMessage()).contains("tmpl_001");
                    });
        }

        @Test
        @DisplayName("승인 템플릿 → 통과")
        void kakao_approved_passes() {
            given(kakaoTemplateService.isApproved(1L, "tmpl_001")).willReturn(true);

            assertThatCode(() -> gate.validate(ctx(SendChannel.KAKAO, "tmpl_001")))
                    .doesNotThrowAnyException();
        }
    }

    // ── order() ───────────────────────────────────────────────────

    @Test
    @DisplayName("order() = 40")
    void order_is40() {
        assertThat(gate.order()).isEqualTo(40);
    }
}
