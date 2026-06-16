package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.service.template.RcsTemplateService;
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
@DisplayName("RcsTemplateApprovalGate 단위 테스트")
class RcsTemplateApprovalGateTest {

    @Mock
    private RcsTemplateService rcsTemplateService;

    @InjectMocks
    private RcsTemplateApprovalGate gate;

    private SendValidationContext ctx(SendChannel channel, String templateCode) {
        return new SendValidationContext(
                1L, 10L, ApiKeyType.TEST, "01012345678",
                channel, "RCS 메시지", false, 1, 10L, NetworkType.TEST, templateCode);
    }

    // ── 비RCS 채널 — 스킵 ─────────────────────────────────────────

    @Nested
    @DisplayName("비RCS 채널")
    class NonRcsChannelTest {

        @Test
        @DisplayName("SMS 채널 → 검증 없이 통과")
        void sms_skipsValidation() {
            assertThatCode(() -> gate.validate(ctx(SendChannel.SMS, null)))
                    .doesNotThrowAnyException();
            verify(rcsTemplateService, never()).isApproved(1L, null);
        }

        @Test
        @DisplayName("KAKAO 채널 → 검증 없이 통과")
        void kakao_skipsValidation() {
            assertThatCode(() -> gate.validate(ctx(SendChannel.KAKAO, "tmpl_001")))
                    .doesNotThrowAnyException();
        }
    }

    // ── RCS 채널 ──────────────────────────────────────────────────

    @Nested
    @DisplayName("RCS 채널")
    class RcsChannelTest {

        @Test
        @DisplayName("templateCode(messagebaseId) 없음 → TEMPLATE_REQUIRED")
        void rcs_noTemplateCode_throws() {
            assertThatThrownBy(() -> gate.validate(ctx(SendChannel.RCS, null)))
                    .isInstanceOf(SendValidationException.class)
                    .satisfies(ex -> {
                        SendValidationException sve = (SendValidationException) ex;
                        assertThat(sve.getErrorCode()).isEqualTo(SendErrorCode.TEMPLATE_REQUIRED);
                    });
        }

        @Test
        @DisplayName("미승인 RCS 템플릿 → TEMPLATE_NOT_APPROVED")
        void rcs_notApproved_throws() {
            given(rcsTemplateService.isApproved(1L, "rcs_001")).willReturn(false);

            assertThatThrownBy(() -> gate.validate(ctx(SendChannel.RCS, "rcs_001")))
                    .isInstanceOf(SendValidationException.class)
                    .satisfies(ex -> {
                        SendValidationException sve = (SendValidationException) ex;
                        assertThat(sve.getErrorCode()).isEqualTo(SendErrorCode.TEMPLATE_NOT_APPROVED);
                        assertThat(sve.getMessage()).contains("rcs_001");
                    });
        }

        @Test
        @DisplayName("승인된 RCS 템플릿 → 통과")
        void rcs_approved_passes() {
            given(rcsTemplateService.isApproved(1L, "rcs_001")).willReturn(true);

            assertThatCode(() -> gate.validate(ctx(SendChannel.RCS, "rcs_001")))
                    .doesNotThrowAnyException();
        }
    }

    // ── order() ───────────────────────────────────────────────────

    @Test
    @DisplayName("order() = 41")
    void order_is41() {
        assertThat(gate.order()).isEqualTo(41);
    }
}
