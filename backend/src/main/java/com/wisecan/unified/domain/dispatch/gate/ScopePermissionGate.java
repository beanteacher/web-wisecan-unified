package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 검증 2 — API Key 스코프 권한.
 * API Key 상태가 ACTIVE 이고 발송 채널에 대응하는 스코프를 보유해야 한다.
 * SEND(전채널) 또는 채널별 스코프(SEND_SMS / SEND_KAKAO / SEND_RCS) 중 하나 보유 시 통과.
 * 02_FEATURE_SPEC.md §5.3 / 05_DATA_MODEL.md §4.2 참조.
 */
@Component
@RequiredArgsConstructor
public class ScopePermissionGate implements SendValidationGate {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public void validate(SendValidationContext ctx) {
        ApiKey apiKey = apiKeyRepository.findById(ctx.apiKeyId())
                .orElseThrow(() -> new EntityNotFoundException("API Key를 찾을 수 없습니다."));

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            throw new SendValidationException(SendErrorCode.API_KEY_REVOKED);
        }
        if (apiKey.getStatus() != ApiKeyStatus.ACTIVE) {
            throw new SendValidationException(SendErrorCode.API_KEY_NOT_ACTIVE);
        }

        // SEND(전채널) 또는 채널별 스코프 중 하나 보유하면 통과
        boolean hasSendAll = apiKey.hasScope(ApiKeyScope.SEND);
        boolean hasChannelScope = apiKey.hasScope(ctx.channel().getChannelScope());

        if (!hasSendAll && !hasChannelScope) {
            throw new SendValidationException(SendErrorCode.SCOPE_NOT_GRANTED,
                    "이 API 키에 '" + ctx.channel().getDisplayName() + "' 발송 스코프가 없습니다.");
        }
    }

    @Override
    public int order() {
        return 20;
    }
}
