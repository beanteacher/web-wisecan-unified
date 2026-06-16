package com.wisecan.unified.domain.template;

/**
 * 외부 시스템 kko_template 조회 결과 전달 객체.
 * 05_DATA_MODEL §6.1.2 컬럼 명세 기반.
 * 중계사 식별자(kko_profile_no) 등 라우팅 정보는 회원에게 노출하지 않는다 (INV-02).
 *
 * @param templateCode       템플릿 코드
 * @param templateName       템플릿명
 * @param templateContent    템플릿 내용
 * @param inspectionStatus   검수 상태
 * @param templateStatus     템플릿 사용 상태
 * @param messageType        메시지 유형 (BA/EX/AD/MI)
 * @param categoryCode       카테고리 코드
 * @param buttons            버튼 JSON
 * @param securityFlag       보안 템플릿 여부
 */
public record KakaoTemplateInfo(
        String templateCode,
        String templateName,
        String templateContent,
        KakaoInspectionStatus inspectionStatus,
        KakaoTemplateStatus templateStatus,
        String messageType,
        String categoryCode,
        String buttons,
        boolean securityFlag
) {
    /** 발송 가능 상태 — 검수 승인(APR) + 정상(A) */
    public boolean isSendable() {
        return inspectionStatus != null && inspectionStatus.isApproved()
                && templateStatus != null && templateStatus.isActive();
    }
}
