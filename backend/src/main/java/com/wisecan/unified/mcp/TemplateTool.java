package com.wisecan.unified.mcp;

import com.wisecan.unified.common.security.ApiKeyPrincipal;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.service.template.KakaoTemplateService;
import com.wisecan.unified.service.template.RcsTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * wsc.template.kakao.list / wsc.template.rcs.brands / wsc.template.rcs.list
 * — 템플릿 조회 도구 (RQ-MCP-012, 013).
 *
 * 비노출 정책 (§7.3): 템플릿 등록·수정·삭제는 MCP 도구로 노출하지 않는다.
 * 조회(template:read 스코프)만 허용.
 * 동등성 (§7.4): CLI wsc template list --type kakao|rcs 와 동일 결과.
 *
 * W-305 서비스 연동 완료 버전.
 */
@Component
@RequiredArgsConstructor
public class TemplateTool {

    private final KakaoTemplateService kakaoTemplateService;
    private final RcsTemplateService rcsTemplateService;
    private final MemberRepository memberRepository;

    @Tool(description = """
            카카오 알림톡 템플릿 목록을 조회한다.
            필수 스코프: template:read
            응답: [{ templateCode, templateName, inspectionStatus, sendable }]
            (주의: 템플릿 등록·수정·삭제는 웹 콘솔 전용입니다.)
            """)
    public List<TemplateDto.KakaoTemplateResponse> templateKakaoList(
            @ToolParam(description = "페이지 번호 (0-based, 현재 미사용)") Integer page,
            @ToolParam(description = "페이지 크기 (현재 미사용)") Integer size
    ) {
        String email = resolveEmail();
        return kakaoTemplateService.list(email);
    }

    @Tool(description = """
            회원이 보유한 RCS 브랜드 ID 목록을 조회한다.
            필수 스코프: template:read
            응답: ["brandId1", "brandId2", ...]
            """)
    public List<String> templateRcsBrands() {
        String email = resolveEmail();
        return rcsTemplateService.listBrands(email);
    }

    @Tool(description = """
            특정 RCS 브랜드의 템플릿 목록을 조회한다.
            필수 스코프: template:read
            응답: [{ messagebaseId, templateName, approvalResult, sendable }]
            (주의: 템플릿 등록·수정·삭제는 웹 콘솔 전용입니다.)
            """)
    public List<TemplateDto.RcsTemplateResponse> templateRcsList(
            @ToolParam(description = "RCS 브랜드 ID") String brandId
    ) {
        String email = resolveEmail();
        return rcsTemplateService.listByBrand(email, brandId);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    /**
     * 현재 SecurityContext 의 ApiKey 인증에서 memberId 를 추출해
     * MemberRepository 를 통해 email 을 조회한다.
     * 서비스 레이어 기존 시그니처(email 기반)와 정합성을 유지한다.
     */
    private String resolveEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof ApiKeyPrincipal apiKey)) {
            throw new IllegalStateException("MCP 템플릿 도구는 API Key 인증이 필요합니다.");
        }
        Member member = memberRepository.findById(apiKey.memberId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "회원을 찾을 수 없습니다: memberId=" + apiKey.memberId()));
        return member.getEmail();
    }
}
