package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import org.springframework.stereotype.Component;

/**
 * 검증 6 — 광고 의무 표기 검사.
 * 광고성 메시지({@code isAdvertisement = true})인 경우:
 * <ol>
 *   <li>메시지 앞에 '(광고)' 표기가 있어야 한다.</li>
 *   <li>메시지 끝에 080 수신거부 번호 안내가 있어야 한다.</li>
 * </ol>
 * 02_FEATURE_SPEC.md §13.1, 정보통신망법 §50 광고성 정보 전송 규제 참조.
 */
@Component
public class AdDisclosureGate implements SendValidationGate {

    /** 광고 표기 필수 접두사 */
    private static final String AD_PREFIX = "(광고)";

    /** 080 수신거부 번호 패턴 (예: 080-000-0000 또는 0800000000) */
    private static final String OPT_OUT_PATTERN = "080";

    @Override
    public void validate(SendValidationContext ctx) {
        if (!ctx.isAdvertisement()) {
            return;
        }

        String body = ctx.messageBody();
        if (body == null || body.isBlank()) {
            throw new SendValidationException(SendErrorCode.AD_DISCLOSURE_MISSING,
                    "광고 메시지 본문이 비어 있습니다.");
        }

        // 1. '(광고)' 표기 검사
        if (!body.startsWith(AD_PREFIX)) {
            throw new SendValidationException(SendErrorCode.AD_DISCLOSURE_MISSING,
                    "광고 메시지는 '" + AD_PREFIX + "'로 시작해야 합니다.");
        }

        // 2. 080 수신거부 번호 안내 검사
        if (!body.contains(OPT_OUT_PATTERN)) {
            throw new SendValidationException(SendErrorCode.AD_OPT_OUT_MISSING,
                    "광고 메시지 끝에 080 수신거부 번호 안내가 필요합니다.");
        }
    }

    @Override
    public int order() {
        return 60;
    }
}
