package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * wsc.brand.rcs.list — RCS 브랜드 조회 도구 (RQ-MCP-014).
 *
 * 비노출 정책 (§7.3): 브랜드 등록·수정·삭제는 MCP 도구로 노출하지 않는다.
 * 조회(brand:read 스코프)만 허용.
 * 동등성 (§7.4): CLI wsc brand list 와 동일 결과.
 *
 * W-305(카카오·RCS 템플릿/브랜드 서비스) 구현 후 실제 서비스 주입으로 대체한다.
 */
@Component
@RequiredArgsConstructor
public class BrandTool {

    @Tool(description = """
            RCS 브랜드 목록을 조회한다.
            필수 스코프: brand:read
            응답: [{ brandId, brandName, status, logoUrl, description }]
            (주의: 브랜드 등록·수정·삭제는 웹 콘솔 전용입니다.)
            """)
    public List<Map<String, Object>> brandRcsList(
            @ToolParam(description = "페이지 번호 (0-based, 기본 0)") Integer page,
            @ToolParam(description = "페이지 크기 (기본 20)") Integer size
    ) {
        requireApiKeyAuth();
        // W-305 RcsBrandService 연동 전 안내 응답
        return List.of(Map.of(
                "note", "RCS 브랜드 서비스(W-305)가 연동되면 실제 목록이 반환됩니다.",
                "page", page != null ? page : 0,
                "size", size != null ? size : 20
        ));
    }

    private void requireApiKeyAuth() {
        Object auth = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(auth instanceof ApiKeyPrincipal)) {
            throw new IllegalStateException("MCP 브랜드 도구는 API Key 인증이 필요합니다.");
        }
    }
}
