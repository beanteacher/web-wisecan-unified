package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 검증 3 — 발신번호 화이트리스트.
 * API Key에 발신번호 화이트리스트가 설정된 경우, 요청 발신번호가 그 목록에 포함되어야 한다.
 * 화이트리스트가 없으면(null/blank) 제한 없음으로 통과.
 * 05_DATA_MODEL.md §4.2 API_KEY_CALLBACK_WHITELIST 참조.
 */
@Component
@RequiredArgsConstructor
public class CallerWhitelistGate implements SendValidationGate {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public void validate(SendValidationContext ctx) {
        ApiKey apiKey = apiKeyRepository.findById(ctx.apiKeyId())
                .orElseThrow(() -> new EntityNotFoundException("API Key를 찾을 수 없습니다."));

        String allowedRaw = apiKey.getAllowedCallbacksRaw();
        if (allowedRaw == null || allowedRaw.isBlank()) {
            // 화이트리스트 미설정 — 제한 없음
            return;
        }

        String normalizedCallback = ctx.callbackNumber().replaceAll("[^0-9]", "");
        boolean allowed = false;
        for (String entry : allowedRaw.split(",")) {
            String normalized = entry.trim().replaceAll("[^0-9]", "");
            if (normalized.equals(normalizedCallback)) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            throw new SendValidationException(SendErrorCode.CALLER_NOT_IN_WHITELIST,
                    "발신번호 '" + ctx.callbackNumber() + "'은(는) 이 API 키의 허용 목록에 없습니다.");
        }
    }

    @Override
    public int order() {
        return 30;
    }
}
