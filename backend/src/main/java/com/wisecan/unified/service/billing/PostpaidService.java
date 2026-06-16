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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 후불 모델 서비스 — W-403.
 *
 * 주요 흐름:
 *   1. 사업자 회원이 후불 신청(APPLIED) 생성
 *   2. 운영자가 심사 시작(UNDER_REVIEW) → 승인(ACTIVE) 확정
 *      — 신용 한도·보증보험 증권번호 기록
 *   3. 정산 주기마다 청구서 발행 (운영자 또는 배치)
 *      — lock:invoice:{companyId}:{period} Redis 5분 락으로 중복 방지
 *   4. 회원이 청구서 결제 — PAID 시 PostpaidBlockGate 차단 해제
 *   5. 납부 기한 경과 → 배치가 markOverdue() 호출 → OVERDUE 상태
 *      — PostpaidBlockGate 가 OVERDUE 청구서 조회해 발송 차단
 *
 * 02_FEATURE_SPEC §10.3 / 05_DATA_MODEL §7.4 참조.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostpaidService {

    private static final String LOCK_INVOICE_PREFIX = "lock:invoice:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final PostpaidConfigRepository postpaidConfigRepository;
    private final PostpaidInvoiceRepository postpaidInvoiceRepository;
    private final StringRedisTemplate redisTemplate;

    // ──────────────────────────────────────────
    // 후불 신청
    // ──────────────────────────────────────────

    /**
     * 후불 신청 — 사업자 회원 한정.
     * 이미 APPLIED / UNDER_REVIEW / ACTIVE 상태이면 중복 신청 차단.
     *
     * @param companyId 회사 ID
     * @param request   신청 정보
     */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.ConfigResponse apply(Long companyId, PostpaidDto.ApplyRequest request) {
        boolean alreadyExists = postpaidConfigRepository.findByCompanyId(companyId)
                .map(c -> c.getStatus() != PostpaidStatus.SUSPENDED)
                .orElse(false);

        if (alreadyExists) {
            throw new BillingException("이미 후불 신청이 존재합니다. 현재 상태를 확인해 주세요.");
        }

        PostpaidConfig config = PostpaidConfig.builder()
                .companyId(companyId)
                .billingCycle(request.billingCycle() != null ? request.billingCycle() : PostpaidBillingCycle.MONTHLY)
                .creditLimit(null)
                .guaranteeInsuranceNo(request.guaranteeInsuranceNo())
                .build();

        config = postpaidConfigRepository.save(config);
        log.info("[후불 신청] companyId={} configId={}", companyId, config.getId());
        return PostpaidDto.ConfigResponse.from(config);
    }

    // ──────────────────────────────────────────
    // 운영자 심사
    // ──────────────────────────────────────────

    /** 심사 시작 — APPLIED → UNDER_REVIEW */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.ConfigResponse startReview(Long configId) {
        PostpaidConfig config = findConfigOrThrow(configId);
        config.startReview();
        log.info("[후불 심사 시작] configId={}", configId);
        return PostpaidDto.ConfigResponse.from(config);
    }

    /** 심사 승인 — UNDER_REVIEW → ACTIVE */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.ConfigResponse approve(Long configId, PostpaidDto.ApproveRequest request) {
        PostpaidConfig config = findConfigOrThrow(configId);
        config.approve(request.creditLimit(), request.guaranteeInsuranceNo());
        log.info("[후불 승인] configId={} creditLimit={}", configId, request.creditLimit());
        return PostpaidDto.ConfigResponse.from(config);
    }

    /** 운영자 정지 — ACTIVE → SUSPENDED */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.ConfigResponse suspend(Long configId) {
        PostpaidConfig config = findConfigOrThrow(configId);
        config.suspend();
        log.info("[후불 정지] configId={}", configId);
        return PostpaidDto.ConfigResponse.from(config);
    }

    // ──────────────────────────────────────────
    // 청구서 발행
    // ──────────────────────────────────────────

    /**
     * 청구서 발행 — 운영자 또는 배치 호출.
     * Redis 락(lock:invoice:{companyId}:{period}, TTL 5분)으로 중복 발행 방지.
     * 동일 postpaidConfigId + periodLabel 이면 UniqueConstraint 로도 차단.
     */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.InvoiceResponse issueInvoice(Long companyId, PostpaidDto.IssueInvoiceRequest request) {
        PostpaidConfig config = postpaidConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new EntityNotFoundException("후불 설정을 찾을 수 없습니다: companyId=" + companyId));

        if (!config.isActive()) {
            throw new BillingException("후불 활성(ACTIVE) 상태가 아니면 청구서를 발행할 수 없습니다.");
        }

        String lockKey = LOCK_INVOICE_PREFIX + companyId + ":" + request.periodLabel();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (Boolean.FALSE.equals(locked)) {
            throw new BillingException("청구서 발행이 이미 진행 중입니다. 잠시 후 다시 시도해 주세요.");
        }

        try {
            if (postpaidInvoiceRepository
                    .findByPostpaidConfigIdAndPeriodLabel(config.getId(), request.periodLabel())
                    .isPresent()) {
                throw new BillingException("해당 기간의 청구서가 이미 존재합니다: " + request.periodLabel());
            }

            PostpaidInvoice invoice = PostpaidInvoice.builder()
                    .postpaidConfigId(config.getId())
                    .periodLabel(request.periodLabel())
                    .totalAmount(request.totalAmount())
                    .issuedAt(LocalDateTime.now())
                    .dueAt(request.dueAt())
                    .build();

            invoice = postpaidInvoiceRepository.save(invoice);
            log.info("[청구서 발행] companyId={} period={} amount={} invoiceId={}",
                    companyId, request.periodLabel(), request.totalAmount(), invoice.getId());
            return PostpaidDto.InvoiceResponse.from(invoice);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    // ──────────────────────────────────────────
    // 청구서 결제
    // ──────────────────────────────────────────

    /**
     * 청구서 결제 — 회원 호출.
     * 완납 시 PAID 전환 → PostpaidBlockGate 차단 해제.
     */
    @Transactional(rollbackFor = Exception.class)
    public PostpaidDto.InvoiceResponse payInvoice(Long invoiceId, PostpaidDto.PayInvoiceRequest request) {
        PostpaidInvoice invoice = findInvoiceOrThrow(invoiceId);
        invoice.pay(request.amount());
        log.info("[청구서 결제] invoiceId={} amount={} status={}", invoiceId, request.amount(), invoice.getStatus());
        return PostpaidDto.InvoiceResponse.from(invoice);
    }

    // ──────────────────────────────────────────
    // 연체 처리 (배치)
    // ──────────────────────────────────────────

    /**
     * 연체 일괄 처리 — 스케줄러·배치에서 주기적으로 호출.
     * ISSUED 상태이고 dueAt 이 현재 시각 이전인 청구서를 OVERDUE 로 전환.
     * OVERDUE 청구서가 있으면 PostpaidBlockGate 가 해당 회사의 발송을 차단.
     *
     * @return 연체 처리된 청구서 수
     */
    @Transactional(rollbackFor = Exception.class)
    public int processOverdue() {
        List<PostpaidInvoice> targets = postpaidInvoiceRepository.findOverdueTargets(LocalDateTime.now());
        targets.forEach(PostpaidInvoice::markOverdue);
        log.info("[연체 처리] 처리 건수={}", targets.size());
        return targets.size();
    }

    // ──────────────────────────────────────────
    // 조회
    // ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public PostpaidDto.ConfigResponse getConfig(Long companyId) {
        PostpaidConfig config = postpaidConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new EntityNotFoundException("후불 설정을 찾을 수 없습니다: companyId=" + companyId));
        return PostpaidDto.ConfigResponse.from(config);
    }

    @Transactional(readOnly = true)
    public List<PostpaidDto.InvoiceResponse> listInvoices(Long companyId) {
        PostpaidConfig config = postpaidConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new EntityNotFoundException("후불 설정을 찾을 수 없습니다: companyId=" + companyId));
        return postpaidInvoiceRepository
                .findByPostpaidConfigIdOrderByIssuedAtDesc(config.getId())
                .stream()
                .map(PostpaidDto.InvoiceResponse::from)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────

    private PostpaidConfig findConfigOrThrow(Long configId) {
        return postpaidConfigRepository.findById(configId)
                .orElseThrow(() -> new EntityNotFoundException("후불 설정을 찾을 수 없습니다: id=" + configId));
    }

    private PostpaidInvoice findInvoiceOrThrow(Long invoiceId) {
        return postpaidInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("청구서를 찾을 수 없습니다: id=" + invoiceId));
    }
}
