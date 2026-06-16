package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 검증 7 — 야간 광고 발송 차단.
 * 광고성 메시지를 야간(21:00 ~ 익일 08:00)에 발송하려 하면 차단한다.
 * 정보통신망법 §50조의8(야간 광고성 정보 전송 제한) 준수.
 * 02_FEATURE_SPEC.md §13.1, NFR-COMP-003 참조.
 */
@Component
public class NightAdBlockGate implements SendValidationGate {

    /** 야간 시작 시각 (21:00) */
    private static final LocalTime NIGHT_START = LocalTime.of(21, 0);
    /** 야간 종료 시각 (08:00) */
    private static final LocalTime NIGHT_END = LocalTime.of(8, 0);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public void validate(SendValidationContext ctx) {
        if (!ctx.isAdvertisement()) {
            return;
        }

        LocalTime now = LocalTime.now(KST);
        if (isNightTime(now)) {
            throw new SendValidationException(SendErrorCode.AD_NIGHT_BLOCKED,
                    "야간(21:00 ~ 08:00)에는 광고성 메시지를 발송할 수 없습니다.");
        }
    }

    /**
     * 야간 시간대 여부 판단.
     * 21:00 이상이거나 08:00 미만이면 야간.
     */
    static boolean isNightTime(LocalTime time) {
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END);
    }

    @Override
    public int order() {
        return 70;
    }
}
