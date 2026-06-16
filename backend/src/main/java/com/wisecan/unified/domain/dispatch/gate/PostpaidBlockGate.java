package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 검증 9 — 후불 연체 차단.
 * 후불(POSTPAID) 회사 소속 회원이 청구서 연체(OVERDUE) 상태이면 발송을 차단한다.
 * 05_DATA_MODEL.md §7.4 / 02_FEATURE_SPEC.md §10.3 참조.
 *
 * 후불 설정이 없거나 선불 회원이면 통과한다.
 */
@Component
@RequiredArgsConstructor
public class PostpaidBlockGate implements SendValidationGate {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void validate(SendValidationContext ctx) {
        // 회원의 company_id 조회
        Long companyId = jdbcTemplate.queryForObject(
                "SELECT company_id FROM member WHERE id = ?",
                Long.class,
                ctx.memberId());

        if (companyId == null) {
            // 개인 회원 — 후불 없음, 통과
            return;
        }

        // 후불 설정 확인
        String billingMode = jdbcTemplate.queryForObject(
                "SELECT billing_mode FROM company WHERE id = ?",
                String.class,
                companyId);

        if (!"POSTPAID".equals(billingMode)) {
            // 선불 — 통과
            return;
        }

        // 연체 청구서 존재 여부 확인
        Integer overdueCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM postpaid_invoice pi " +
                "JOIN postpaid_config pc ON pi.postpaid_config_id = pc.id " +
                "WHERE pc.company_id = ? AND pi.status = 'OVERDUE'",
                Integer.class,
                companyId);

        if (overdueCount != null && overdueCount > 0) {
            throw new SendValidationException(SendErrorCode.INSUFFICIENT_BALANCE,
                    "미납 청구서가 있어 발송이 차단되었습니다. 청구서를 먼저 결제해 주세요.");
        }
    }

    @Override
    public int order() {
        return 90;
    }
}
