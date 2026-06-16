package com.wisecan.unified.service.trial;

import com.wisecan.unified.domain.trial.TrialDummyContext;
import org.springframework.stereotype.Component;

/**
 * 체험 모드 더미 데이터 팩토리 (W-406).
 *
 * <p>체험 세션 발급 시 사전 적재할 더미 컨텍스트를 생성한다.
 * 더미 값은 고정 하드코딩으로, 실제 회원사 데이터와 무관한 표시용 데이터다.</p>
 */
@Component
public class TrialDummyDataFactory {

    /** 더미 잔액 (원화 가상) */
    private static final long DUMMY_BALANCE = 50_000L;

    /** 더미 발신번호 */
    private static final String DUMMY_CALLBACK = "010-1234-5678";

    /** 더미 API 키 (마스킹 표시) */
    private static final String DUMMY_API_KEY = "wsc_test_****_TRIAL";

    /** 더미 발송 이력 JSON (최근 5건 요약) */
    private static final String DUMMY_SEND_HISTORY_JSON = """
            [
              {"sendId":"TRIAL001","channel":"SMS","recipientNumber":"010-****-1234","status":"DELIVERED","sentAt":"2026-06-15T10:00:00"},
              {"sendId":"TRIAL002","channel":"LMS","recipientNumber":"010-****-5678","status":"DELIVERED","sentAt":"2026-06-15T09:30:00"},
              {"sendId":"TRIAL003","channel":"KAKAO","recipientNumber":"010-****-9012","status":"DELIVERED","sentAt":"2026-06-15T09:00:00"},
              {"sendId":"TRIAL004","channel":"RCS","recipientNumber":"010-****-3456","status":"FAILED","sentAt":"2026-06-15T08:30:00"},
              {"sendId":"TRIAL005","channel":"SMS","recipientNumber":"010-****-7890","status":"DELIVERED","sentAt":"2026-06-15T08:00:00"}
            ]
            """;

    /** 더미 카카오 알림톡 템플릿 JSON */
    private static final String DUMMY_KAKAO_TEMPLATE_JSON = """
            [
              {"templateCode":"TRIAL_NOTI_001","templateName":"[체험] 주문 확인 알림","status":"APPROVED","channel":"KAKAO"},
              {"templateCode":"TRIAL_NOTI_002","templateName":"[체험] 배송 출발 알림","status":"APPROVED","channel":"KAKAO"}
            ]
            """;

    /** 더미 RCS 브랜드 JSON */
    private static final String DUMMY_RCS_BRAND_JSON = """
            {"brandId":"TRIAL_BRAND_001","brandName":"WiseCan 체험 브랜드","status":"REGISTERED"}
            """;

    /**
     * 세션 토큰에 대응하는 더미 컨텍스트를 생성한다.
     *
     * @param sessionToken 체험 세션 토큰
     * @return 적재 준비된 {@link TrialDummyContext}
     */
    public TrialDummyContext buildDummyContext(String sessionToken) {
        return TrialDummyContext.builder()
                .sessionToken(sessionToken)
                .dummyCallbackNumber(DUMMY_CALLBACK)
                .dummyApiKey(DUMMY_API_KEY)
                .dummyBalance(DUMMY_BALANCE)
                .dummySendHistoryJson(DUMMY_SEND_HISTORY_JSON)
                .dummyKakaoTemplateJson(DUMMY_KAKAO_TEMPLATE_JSON)
                .dummyRcsBrandJson(DUMMY_RCS_BRAND_JSON)
                .build();
    }
}
