package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 검증 1 — 발신번호 등록 여부.
 * 해당 회원이 보유한 발신번호 중 status = REGISTERED 인 행이 존재해야 한다.
 * 05_DATA_MODEL.md §3.3 발송 검증 hot path / §5.6 Redis 캐시 참조.
 */
@Component
@RequiredArgsConstructor
public class CallerRegistrationGate implements SendValidationGate {

    private final CallbackRepository callbackRepository;

    @Override
    public void validate(SendValidationContext ctx) {
        boolean registered = callbackRepository
                .existsByMemberIdAndPhoneNumberAndStatus(
                        ctx.memberId(),
                        ctx.callbackNumber(),
                        CallbackStatus.REGISTERED);
        if (!registered) {
            throw new SendValidationException(SendErrorCode.CALLER_NOT_REGISTERED,
                    "발신번호 '" + ctx.callbackNumber() + "'이(가) 등록되지 않았거나 승인 대기 중입니다.");
        }
    }

    @Override
    public int order() {
        return 10;
    }
}
