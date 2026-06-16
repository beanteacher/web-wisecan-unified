package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.service.security.AbuseDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 검증 5-D — 계정 이상 차단 상태 사전 확인 게이트.
 * AbuseDetectionService 에 의해 차단된 회원의 발송 요청을 모두 차단한다.
 *
 * order=54 — BurstVolumeGate(55), PatternRepeatGate(56), AnomalousHourGate(57) 보다 먼저 실행.
 * 이미 차단된 계정은 추가 탐지 로직 없이 즉시 거부한다.
 *
 * 02_FEATURE_SPEC.md §13.2, RQ-SEC-007 참조.
 */
@Component
@RequiredArgsConstructor
public class AccountAbuseBlockGate implements SendValidationGate {

    private final AbuseDetectionService abuseDetectionService;

    @Override
    public void validate(SendValidationContext ctx) {
        if (abuseDetectionService.isBlocked(ctx.memberId())) {
            throw new SendValidationException(SendErrorCode.ACCOUNT_ABUSE_BLOCKED,
                    "보안 정책에 의해 발송이 차단된 계정입니다. 운영자에게 문의하세요.");
        }
    }

    @Override
    public int order() {
        return 54;
    }
}
