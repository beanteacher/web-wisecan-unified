package com.wisecan.unified.domain.dispatch;

import com.wisecan.unified.domain.ApiKeyType;

/**
 * 발송 정합성 검증에 필요한 컨텍스트.
 * 검증 파이프라인(SendValidationService)에 전달되는 불변 입력 객체.
 *
 * @param memberId       발송 요청자 회원 ID
 * @param apiKeyId       인증에 사용된 API Key ID
 * @param apiKeyType     API Key 유형 — TEST / PRODUCTION (W-205 망 분리 판별)
 * @param callbackNumber 발신번호 (E.164 정규화 형태, 예: "01012345678")
 * @param channel        발송 채널
 * @param messageBody    메시지 본문 (스팸·광고 검사 대상)
 * @param isAdvertisement 광고성 메시지 여부 (회원이 명시하거나 본문 자동 감지)
 * @param recipientCount 수신자 수 (잔액 사전 평가에 사용)
 * @param unitCost       건당 발송 단가 (원화, 잔액 사전 평가에 사용)
 * @param networkType    요청된 발송 망 유형 — TEST / PRODUCTION (W-205)
 */
public record SendValidationContext(
        Long memberId,
        Long apiKeyId,
        ApiKeyType apiKeyType,
        String callbackNumber,
        SendChannel channel,
        String messageBody,
        boolean isAdvertisement,
        int recipientCount,
        long unitCost,
        NetworkType networkType
) {
    /** 총 예상 차감액 */
    public long totalCost() {
        return (long) recipientCount * unitCost;
    }
}
