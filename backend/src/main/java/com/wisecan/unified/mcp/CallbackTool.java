package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.common.security.UserPrincipal;
import com.wisecan.unified.dto.sendernumber.CallbackDto;
import com.wisecan.unified.service.sendernumber.CallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * wsc.callback.list — 발신번호 조회 도구 (RQ-MCP-008).
 *
 * <p>비노출 정책 (§7.3): 발신번호 등록·삭제는 MCP 도구로 노출하지 않는다.
 * 조회(callback:read 스코프)만 허용.</p>
 *
 * <p>동등성 (§7.4): CallbackService.list() 를 직접 호출하여 CLI wsc callback list 와 동일 결과.</p>
 */
@Component
@RequiredArgsConstructor
public class CallbackTool {

    private final CallbackService callbackService;

    @Tool(description = """
            등록된 발신번호 목록을 조회한다.
            필수 스코프: callback:read
            응답: [{ id, phoneNumber, status, registerType, description, createdAt }]
            (주의: 발신번호 등록·삭제는 웹 콘솔 또는 CLI 전용입니다. MCP 도구로는 조회만 가능합니다.)
            """)
    public List<CallbackDto.Summary> callbackList(
            @ToolParam(description = "사용 안 함 — null 전달 (향후 필터 확장 예약)") String unused
    ) {
        String email = resolveEmail();
        return callbackService.list(email);
    }

    private String resolveEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal up) {
            return up.email();
        }
        throw new IllegalStateException("MCP 발신번호 도구는 세션 인증(UserPrincipal)이 필요합니다. API Key 인증만으로는 발신번호 목록을 조회할 수 없습니다.");
    }
}
