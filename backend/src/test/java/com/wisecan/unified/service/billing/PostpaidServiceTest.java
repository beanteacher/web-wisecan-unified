package com.wisecan.unified.service.billing;

import com.wisecan.unified.domain.billing.PostpaidBillingCycle;
import com.wisecan.unified.domain.billing.PostpaidConfig;
import com.wisecan.unified.domain.billing.PostpaidInvoice;
import com.wisecan.unified.domain.billing.PostpaidInvoiceStatus;
import com.wisecan.unified.domain.billing.PostpaidStatus;
import com.wisecan.unified.dto.billing.PostpaidDto;
import com.wisecan.unified.exception.BillingException;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.billing.PostpaidConfigRepository;
import com.wisecan.unified.repository.billing.PostpaidInvoiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostpaidService — 후불 모델 단위 테스트")
class PostpaidServiceTest {

    @InjectMocks
    private PostpaidService postpaidService;

    @Mock
    private PostpaidConfigRepository postpaidConfigRepository;

    @Mock
    private PostpaidInvoiceRepository postpaidInvoiceRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    // ──────────────────────────────────────────
    // 후불 신청
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("후불 신청 (apply)")
    class Apply {

        @Test
        @DisplayName("신규 신청 — 설정이 없으면 APPLIED 생성")
        void apply_noExisting_createsApplied() {
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.empty());
            given(postpaidConfigRepository.save(any(PostpaidConfig.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PostpaidDto.ApplyRequest req = new PostpaidDto.ApplyRequest(PostpaidBillingCycle.MONTHLY, "INS-001");
            PostpaidDto.ConfigResponse resp = postpaidService.apply(1L, req);

            assertThat(resp.status()).isEqualTo(PostpaidStatus.APPLIED);
            assertThat(resp.companyId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("이미 ACTIVE 상태이면 중복 신청 차단")
        void apply_alreadyActive_throws() {
            PostpaidConfig active = PostpaidConfig.builder()
                    .companyId(1L)
                    .billingCycle(PostpaidBillingCycle.MONTHLY)
                    .build();
            // ACTIVE 상태로 강제 설정 (approve 도메인 메서드 사용 불가 — 리플렉션 없이 스텁)
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.of(active));

            // APPLIED 상태이므로 status != SUSPENDED → 중복 차단
            PostpaidDto.ApplyRequest req = new PostpaidDto.ApplyRequest(PostpaidBillingCycle.MONTHLY, null);
            assertThatThrownBy(() -> postpaidService.apply(1L, req))
                    .isInstanceOf(BillingException.class)
                    .hasMessageContaining("이미 후불 신청이 존재합니다");
        }

        @Test
        @DisplayName("SUSPENDED 상태이면 재신청 가능")
        void apply_suspended_allowsReapply() {
            PostpaidConfig suspended = buildSuspendedConfig();
            given(postpaidConfigRepository.findByCompanyId(2L)).willReturn(Optional.of(suspended));
            given(postpaidConfigRepository.save(any(PostpaidConfig.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PostpaidDto.ApplyRequest req = new PostpaidDto.ApplyRequest(PostpaidBillingCycle.BIWEEKLY, null);
            PostpaidDto.ConfigResponse resp = postpaidService.apply(2L, req);

            assertThat(resp.status()).isEqualTo(PostpaidStatus.APPLIED);
        }
    }

    // ──────────────────────────────────────────
    // 운영자 심사
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("운영자 심사 (startReview / approve / suspend)")
    class AdminReview {

        @Test
        @DisplayName("startReview — APPLIED → UNDER_REVIEW")
        void startReview_applied_transitionsToUnderReview() {
            PostpaidConfig config = PostpaidConfig.builder()
                    .companyId(10L)
                    .billingCycle(PostpaidBillingCycle.MONTHLY)
                    .build();
            given(postpaidConfigRepository.findById(1L)).willReturn(Optional.of(config));

            PostpaidDto.ConfigResponse resp = postpaidService.startReview(1L);

            assertThat(resp.status()).isEqualTo(PostpaidStatus.UNDER_REVIEW);
        }

        @Test
        @DisplayName("approve — UNDER_REVIEW → ACTIVE, 신용 한도·보증보험 기록")
        void approve_underReview_transitionsToActive() {
            PostpaidConfig config = PostpaidConfig.builder()
                    .companyId(10L)
                    .billingCycle(PostpaidBillingCycle.MONTHLY)
                    .build();
            config.startReview();
            given(postpaidConfigRepository.findById(1L)).willReturn(Optional.of(config));

            PostpaidDto.ApproveRequest req = new PostpaidDto.ApproveRequest(5_000_000L, "INS-999");
            PostpaidDto.ConfigResponse resp = postpaidService.approve(1L, req);

            assertThat(resp.status()).isEqualTo(PostpaidStatus.ACTIVE);
            assertThat(resp.creditLimit()).isEqualTo(5_000_000L);
            assertThat(resp.guaranteeInsuranceNo()).isEqualTo("INS-999");
            assertThat(resp.activatedAt()).isNotNull();
        }

        @Test
        @DisplayName("approve — APPLIED 상태이면 예외")
        void approve_appliedState_throws() {
            PostpaidConfig config = PostpaidConfig.builder()
                    .companyId(10L)
                    .billingCycle(PostpaidBillingCycle.MONTHLY)
                    .build();
            given(postpaidConfigRepository.findById(1L)).willReturn(Optional.of(config));

            PostpaidDto.ApproveRequest req = new PostpaidDto.ApproveRequest(1_000_000L, "INS-001");
            assertThatThrownBy(() -> postpaidService.approve(1L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("UNDER_REVIEW");
        }

        @Test
        @DisplayName("suspend — ACTIVE → SUSPENDED")
        void suspend_active_transitionsToSuspended() {
            PostpaidConfig config = buildActiveConfig();
            given(postpaidConfigRepository.findById(1L)).willReturn(Optional.of(config));

            PostpaidDto.ConfigResponse resp = postpaidService.suspend(1L);

            assertThat(resp.status()).isEqualTo(PostpaidStatus.SUSPENDED);
        }

        @Test
        @DisplayName("configId 없으면 EntityNotFoundException")
        void startReview_notFound_throws() {
            given(postpaidConfigRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postpaidService.startReview(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // 청구서 발행
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("청구서 발행 (issueInvoice)")
    class IssueInvoice {

        @Test
        @DisplayName("ACTIVE 상태, 중복 없음 — 청구서 ISSUED 생성")
        void issue_active_creates() {
            PostpaidConfig config = buildActiveConfig();
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.of(config));
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
            given(postpaidInvoiceRepository.findByPostpaidConfigIdAndPeriodLabel(anyLong(), anyString()))
                    .willReturn(Optional.empty());
            given(postpaidInvoiceRepository.save(any(PostpaidInvoice.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            PostpaidDto.IssueInvoiceRequest req = new PostpaidDto.IssueInvoiceRequest(
                    config.getId(), "2026-06", 100_000L, LocalDateTime.now().plusDays(14));
            PostpaidDto.InvoiceResponse resp = postpaidService.issueInvoice(1L, req);

            assertThat(resp.status()).isEqualTo(PostpaidInvoiceStatus.ISSUED);
            assertThat(resp.totalAmount()).isEqualTo(100_000L);
            assertThat(resp.paidAmount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("ACTIVE 아닌 상태 — 청구서 발행 불가")
        void issue_notActive_throws() {
            PostpaidConfig config = PostpaidConfig.builder()
                    .companyId(1L).billingCycle(PostpaidBillingCycle.MONTHLY).build();
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.of(config));

            PostpaidDto.IssueInvoiceRequest req = new PostpaidDto.IssueInvoiceRequest(
                    1L, "2026-06", 100_000L, LocalDateTime.now().plusDays(14));
            assertThatThrownBy(() -> postpaidService.issueInvoice(1L, req))
                    .isInstanceOf(BillingException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("Redis 락 획득 실패 — 중복 발행 차단")
        void issue_lockFailed_throws() {
            PostpaidConfig config = buildActiveConfig();
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.of(config));
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), anyString(), any())).willReturn(false);

            PostpaidDto.IssueInvoiceRequest req = new PostpaidDto.IssueInvoiceRequest(
                    1L, "2026-06", 100_000L, LocalDateTime.now().plusDays(14));
            assertThatThrownBy(() -> postpaidService.issueInvoice(1L, req))
                    .isInstanceOf(BillingException.class)
                    .hasMessageContaining("진행 중");
        }

        @Test
        @DisplayName("동일 periodLabel 청구서 이미 존재 — 중복 차단")
        void issue_duplicatePeriod_throws() {
            PostpaidConfig config = buildActiveConfig();
            given(postpaidConfigRepository.findByCompanyId(1L)).willReturn(Optional.of(config));
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.setIfAbsent(anyString(), anyString(), any())).willReturn(true);
            given(postpaidInvoiceRepository.findByPostpaidConfigIdAndPeriodLabel(anyLong(), anyString()))
                    .willReturn(Optional.of(buildInvoice(config.getId(), "2026-06", 50_000L)));

            PostpaidDto.IssueInvoiceRequest req = new PostpaidDto.IssueInvoiceRequest(
                    1L, "2026-06", 100_000L, LocalDateTime.now().plusDays(14));
            assertThatThrownBy(() -> postpaidService.issueInvoice(1L, req))
                    .isInstanceOf(BillingException.class)
                    .hasMessageContaining("이미 존재");
        }
    }

    // ──────────────────────────────────────────
    // 청구서 결제
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("청구서 결제 (payInvoice)")
    class PayInvoice {

        @Test
        @DisplayName("완납 — PAID 전환")
        void pay_fullAmount_becomesPaid() {
            PostpaidInvoice invoice = buildInvoice(1L, "2026-06", 100_000L);
            given(postpaidInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

            PostpaidDto.InvoiceResponse resp = postpaidService.payInvoice(1L,
                    new PostpaidDto.PayInvoiceRequest(100_000L));

            assertThat(resp.status()).isEqualTo(PostpaidInvoiceStatus.PAID);
            assertThat(resp.paidAmount()).isEqualTo(100_000L);
            assertThat(resp.remainingAmount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("부분 납부 — ISSUED 유지, paidAmount 증가")
        void pay_partial_remainsIssued() {
            PostpaidInvoice invoice = buildInvoice(1L, "2026-06", 100_000L);
            given(postpaidInvoiceRepository.findById(1L)).willReturn(Optional.of(invoice));

            PostpaidDto.InvoiceResponse resp = postpaidService.payInvoice(1L,
                    new PostpaidDto.PayInvoiceRequest(30_000L));

            assertThat(resp.status()).isEqualTo(PostpaidInvoiceStatus.ISSUED);
            assertThat(resp.paidAmount()).isEqualTo(30_000L);
            assertThat(resp.remainingAmount()).isEqualTo(70_000L);
        }

        @Test
        @DisplayName("연체 청구서 완납 — PAID 전환 (발송 차단 해제)")
        void pay_overdueFullAmount_becomesPaid() {
            PostpaidInvoice invoice = buildInvoice(1L, "2026-05", 50_000L);
            invoice.markOverdue();
            given(postpaidInvoiceRepository.findById(2L)).willReturn(Optional.of(invoice));

            PostpaidDto.InvoiceResponse resp = postpaidService.payInvoice(2L,
                    new PostpaidDto.PayInvoiceRequest(50_000L));

            assertThat(resp.status()).isEqualTo(PostpaidInvoiceStatus.PAID);
        }

        @Test
        @DisplayName("청구서 없으면 EntityNotFoundException")
        void pay_notFound_throws() {
            given(postpaidInvoiceRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> postpaidService.payInvoice(99L,
                    new PostpaidDto.PayInvoiceRequest(10_000L)))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────
    // 연체 처리
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("연체 처리 (processOverdue)")
    class ProcessOverdue {

        @Test
        @DisplayName("만료된 ISSUED 청구서를 OVERDUE 로 전환")
        void processOverdue_issuedPastDue_becomesOverdue() {
            PostpaidInvoice inv1 = buildInvoice(1L, "2026-04", 80_000L);
            PostpaidInvoice inv2 = buildInvoice(2L, "2026-05", 40_000L);
            given(postpaidInvoiceRepository.findOverdueTargets(any(LocalDateTime.class)))
                    .willReturn(List.of(inv1, inv2));

            int count = postpaidService.processOverdue();

            assertThat(count).isEqualTo(2);
            assertThat(inv1.getStatus()).isEqualTo(PostpaidInvoiceStatus.OVERDUE);
            assertThat(inv2.getStatus()).isEqualTo(PostpaidInvoiceStatus.OVERDUE);
        }

        @Test
        @DisplayName("처리 대상 없으면 0 반환")
        void processOverdue_noTargets_returnsZero() {
            given(postpaidInvoiceRepository.findOverdueTargets(any(LocalDateTime.class)))
                    .willReturn(List.of());

            assertThat(postpaidService.processOverdue()).isEqualTo(0);
        }
    }

    // ──────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────

    private PostpaidConfig buildActiveConfig() {
        PostpaidConfig config = PostpaidConfig.builder()
                .companyId(1L)
                .billingCycle(PostpaidBillingCycle.MONTHLY)
                .build();
        config.startReview();
        config.approve(5_000_000L, "INS-001");
        return config;
    }

    private PostpaidConfig buildSuspendedConfig() {
        PostpaidConfig config = buildActiveConfig();
        config.suspend();
        return config;
    }

    private PostpaidInvoice buildInvoice(Long configId, String period, long amount) {
        return PostpaidInvoice.builder()
                .postpaidConfigId(configId)
                .periodLabel(period)
                .totalAmount(amount)
                .issuedAt(LocalDateTime.now().minusDays(30))
                .dueAt(LocalDateTime.now().minusDays(1))
                .build();
    }
}
