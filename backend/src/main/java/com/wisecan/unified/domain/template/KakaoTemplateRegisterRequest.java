package com.wisecan.unified.domain.template;

/**
 * 카카오 알림톡 템플릿 등록·수정 요청 파라미터.
 * KakaoTemplateAdapter.registerTemplate / updateTemplate 에 전달된다.
 *
 * @param templateName     템플릿명
 * @param templateContent  템플릿 내용
 * @param messageType      메시지 유형 (BA/EX/AD/MI)
 * @param categoryCodeM    카테고리 마스터 코드
 * @param categoryCodeS    카테고리 서브 코드
 * @param buttons          버튼 JSON (nullable)
 * @param securityFlag     보안 템플릿 여부
 */
public record KakaoTemplateRegisterRequest(
        String templateName,
        String templateContent,
        String messageType,
        String categoryCodeM,
        String categoryCodeS,
        String buttons,
        boolean securityFlag
) {}
