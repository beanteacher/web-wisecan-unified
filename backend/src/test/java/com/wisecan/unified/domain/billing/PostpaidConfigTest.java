package com.wisecan.unified.domain.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PostpaidConfig — 후불 설정 도메인 단위 테스트")
class PostpaidConfigTest {

    private PostpaidConfig buildConfig() {
        return PostpaidConfig.builder()
                .companyId(1L)
                .billingCycle(PostpaidBillingCycle.MONTHLY)
                .build();
    }

    @Test
    @DisplayName("생성 시 status=APPLIED")
    void create_initialStatus_isApplied() {
        PostpaidConfig config = buildConfig();
        assertThat(config.getStatus()).isEqualTo(PostpaidStatus.APPLIED);
        assertThat(config.isActive()).isFalse();
    }

    @Test
    @DisplayName("APPLIED → startReview() → UNDER_REVIEW")
    void startReview_fromApplied_transitionsCorrectly() {
        PostpaidConfig config = buildConfig();

        config.startReview();

        assertThat(config.getStatus()).isEqualTo(PostpaidStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("UNDER_REVIEW → approve() → ACTIVE, 신용 한도·보증보험 기록")
    void approve_fromUnderReview_transitionsToActive() {
        PostpaidConfig config = buildConfig();
        config.startReview();

        config.approve(3_000_000L, "INS-007");

        assertThat(config.getStatus()).isEqualTo(PostpaidStatus.ACTIVE);
        assertThat(config.getCreditLimit()).isEqualTo(3_000_000L);
        assertThat(config.getGuaranteeInsuranceNo()).isEqualTo("INS-007");
        assertThat(config.getActivatedAt()).isNotNull();
        assertThat(config.isActive()).isTrue();
    }

    @Test
    @DisplayName("APPLIED 에서 approve() — UNDER_REVIEW 아니면 예외")
    void approve_fromApplied_throws() {
        PostpaidConfig config = buildConfig();

        assertThatThrownBy(() -> config.approve(1_000_000L, "INS-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNDER_REVIEW");
    }

    @Test
    @DisplayName("APPLIED 에서 startReview() 가 아닌 approve() — 예외")
    void startReview_fromUnderReview_throws() {
        PostpaidConfig config = buildConfig();
        config.startReview();

        assertThatThrownBy(() -> config.startReview())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPLIED");
    }

    @Test
    @DisplayName("ACTIVE → suspend() → SUSPENDED")
    void suspend_fromActive_transitionsToSuspended() {
        PostpaidConfig config = buildConfig();
        config.startReview();
        config.approve(1_000_000L, "INS-X");

        config.suspend();

        assertThat(config.getStatus()).isEqualTo(PostpaidStatus.SUSPENDED);
        assertThat(config.isActive()).isFalse();
    }

    @Test
    @DisplayName("isActive() — ACTIVE 상태만 true")
    void isActive_onlyWhenActive() {
        PostpaidConfig config = buildConfig();
        assertThat(config.isActive()).isFalse();

        config.startReview();
        assertThat(config.isActive()).isFalse();

        config.approve(500_000L, "INS-Y");
        assertThat(config.isActive()).isTrue();

        config.suspend();
        assertThat(config.isActive()).isFalse();
    }
}
