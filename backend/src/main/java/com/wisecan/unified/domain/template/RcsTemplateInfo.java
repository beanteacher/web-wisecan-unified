package com.wisecan.unified.domain.template;

/**
 * 외부 시스템 rcs_template 조회 결과 전달 객체.
 * 05_DATA_MODEL §6.2.1 컬럼 명세 기반.
 *
 * @param messagebaseId   RCS 템플릿 ID
 * @param templateName    템플릿 명칭
 * @param brandId         RCS 브랜드 ID
 * @param usageStatus     사용 상태 (ready/pause)
 * @param approvalResult  승인 상태
 * @param approvalReason  승인 사유
 * @param productCode     메시지 상품 종류 (sms/lms/mms/tmplt)
 * @param spec            레이아웃 구조 (RICHCARD/OPENRICHCARD)
 * @param cardType        카드 종류
 * @param inputText       정보성 텍스트 서술 원본
 */
public record RcsTemplateInfo(
        String messagebaseId,
        String templateName,
        String brandId,
        RcsTemplateUsageStatus usageStatus,
        RcsApprovalResult approvalResult,
        String approvalReason,
        String productCode,
        String spec,
        String cardType,
        String inputText
) {
    /** 발송 가능 상태 — 승인 + ready */
    public boolean isSendable() {
        return approvalResult != null && approvalResult.isApproved()
                && usageStatus != null && usageStatus.isReady();
    }
}
