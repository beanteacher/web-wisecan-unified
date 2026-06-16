package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.service.template.RcsTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 발송 검증 게이트 — RCS 템플릿 승인 상태 확인.
 *
 * 채널이 {@link SendChannel#RCS}일 때만 실행된다.
 * 승인 상태: approval_result = '승인' AND status = 'ready'
 * Redis 캐시(TTL 10분) → MISS 시 외부 DB 조회 순으로 동작한다.
 *
 * 05_DATA_MODEL §6.3~§6.4, 02_FEATURE_SPEC §9.2 참조.
 * order = 41 — KakaoTemplateApprovalGate(40) 직후 실행.
 */
@Component
@RequiredArgsConstructor
public class RcsTemplateApprovalGate implements SendValidationGate {

    private final RcsTemplateService rcsTemplateService;

    @Override
    public void validate(SendValidationContext ctx) {
        if (ctx.channel() != SendChannel.RCS) {
            // RCS 채널이 아니면 통과
            return;
        }

        String messagebaseId = ctx.templateCode();
        if (messagebaseId == null || messagebaseId.isBlank()) {
            throw new SendValidationException(SendErrorCode.TEMPLATE_REQUIRED,
                    "RCS 채널 발송 시 템플릿 ID(messagebaseId)는 필수입니다.");
        }

        boolean approved = rcsTemplateService.isApproved(ctx.memberId(), messagebaseId);
        if (!approved) {
            throw new SendValidationException(SendErrorCode.TEMPLATE_NOT_APPROVED,
                    "RCS 템플릿이 승인되지 않았거나 사용 불가 상태입니다. messagebaseId=" + messagebaseId);
        }
    }

    @Override
    public int order() {
        return 41;
    }
}
