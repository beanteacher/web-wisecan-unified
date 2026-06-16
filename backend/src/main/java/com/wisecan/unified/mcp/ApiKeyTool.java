package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.ApiKeyDto;
import com.wisecan.unified.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * wsc.key.list — API 키 목록 조회 도구 (RQ-MCP-009).
 *
 * <p>비노출 정책 (§7.3): API 키 발급·폐기·재발급·스코프 변경은 MCP 도구로 노출하지 않는다.
 * 조회(key:read 스코프)만 허용.</p>
 *
 * <p>동등성 (§7.4): ApiKeyService.getMyKeys() 를 직접 호출하여 CLI wsc key list 와 동일 결과.</p>
 */
@Component
@RequiredArgsConstructor
public class ApiKeyTool {

    private final ApiKeyService apiKeyService;

    @Tool(description = """
            내 API 키 목록을 조회한다.
            필수 스코프: key:read (또는 로그인 세션)
            응답: [{ id, keyName, keyPrefix, status, keyType, scopes, dailyLimit, createdAt }]
            (주의: rawKey 는 발급 시 1회만 노출됩니다. 이 도구로는 조회할 수 없습니다.)
            (주의: 키 발급·폐기·재발급·스코프 변경은 웹 콘솔 또는 CLI 전용입니다.)
            """)
    public List<ApiKeyDto.Response> keyList(
            @ToolParam(description = "사용 안 함 — null 전달") String unused
    ) {
        Long memberId = resolveMemberId();
        return apiKeyService.getMyKeys(memberId);
    }

    private Long resolveMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof ApiKeyPrincipal akp) {
            return akp.memberId();
        }
        if (principal instanceof UserPrincipal up) {
            return up.memberId();
        }
        throw new IllegalStateException("MCP API 키 도구는 인증이 필요합니다.");
    }
}
