package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.service.template.KakaoTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 발송 검증 게이트 — 카카오 알림톡/친구톡 템플릿 승인 상태 확인.
 *
 * 채널이 {@link SendChannel#KAKAO}일 때만 실행된다.
 * 승인 상태: inspection_status = 'APR' AND status = 'A'
 * Redis 캐시(TTL 10분) → MISS 시 외부 DB 조회 순으로 동작한다.
 *
 * 05_DATA_MODEL §6.3~§6.4, 02_FEATURE_SPEC §9.1 참조.
 * order = 40 — CallerRegistrationGate(10), Balance(20), DailyLimit(30) 이후 실행.
 */
@Component
@RequiredArgsConstructor
public class KakaoTemplateApprovalGate implements SendValidationGate {

    private final KakaoTemplateService kakaoTemplateService;

    @Override
    public void validate(SendValidationContext ctx) {
        if (ctx.channel() != SendChannel.KAKAO) {
            // 카카오 채널이 아니면 통과
            return;
        }

        String templateCode = ctx.templateCode();
        if (templateCode == null || templateCode.isBlank()) {
            throw new SendValidationException(SendErrorCode.TEMPLATE_REQUIRED,
                    "카카오 채널 발송 시 템플릿 코드는 필수입니다.");
        }

        boolean approved = kakaoTemplateService.isApproved(ctx.memberId(), templateCode);
        if (!approved) {
            throw new SendValidationException(SendErrorCode.TEMPLATE_NOT_APPROVED,
                    "카카오 템플릿이 승인되지 않았거나 비활성 상태입니다. templateCode=" + templateCode);
        }
    }

    @Override
    public int order() {
        return 40;
    }
}
