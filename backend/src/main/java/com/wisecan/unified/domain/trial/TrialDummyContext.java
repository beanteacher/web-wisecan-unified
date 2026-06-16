package com.wisecan.unified.domain.trial;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 체험 모드 더미 컨텍스트 엔티티 (W-406).
 *
 * <p>체험 세션에 사전 적재되는 더미 데이터:
 * 더미 발신번호·API 키·잔액·발송 이력·카카오 템플릿·RCS 브랜드 요약을
 * JSON 직렬화 형태로 단일 행에 보관한다.</p>
 *
 * <p>운영 DB 테이블(send_request, cash_balance 등)과 완전 격리되며,
 * 세션 만료/종료 시 이 엔티티도 폐기 대상이 된다.</p>
 */
@Entity
@Table(name = "trial_dummy_context")
@Getter
@NoArgsConstructor
public class TrialDummyContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 체험 세션 토큰 (1:1) */
    @Column(name = "session_token", length = 36, nullable = false, unique = true)
    private String sessionToken;

    /** 더미 발신번호 (예: 010-1234-5678) */
    @Column(name = "dummy_callback_number", length = 20, nullable = false)
    private String dummyCallbackNumber;

    /** 더미 API 키 (표시용 마스킹 값) */
    @Column(name = "dummy_api_key", length = 64, nullable = false)
    private String dummyApiKey;

    /** 더미 잔액 (원화, 체험용 가상 잔액) */
    @Column(name = "dummy_balance", nullable = false)
    private long dummyBalance;

    /** 더미 발송 이력 JSON (최근 5건 요약) */
    @Column(name = "dummy_send_history_json", columnDefinition = "TEXT")
    private String dummySendHistoryJson;

    /** 더미 카카오 알림톡 템플릿 JSON */
    @Column(name = "dummy_kakao_template_json", columnDefinition = "TEXT")
    private String dummyKakaoTemplateJson;

    /** 더미 RCS 브랜드 JSON */
    @Column(name = "dummy_rcs_brand_json", columnDefinition = "TEXT")
    private String dummyRcsBrandJson;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public TrialDummyContext(
            String sessionToken,
            String dummyCallbackNumber,
            String dummyApiKey,
            long dummyBalance,
            String dummySendHistoryJson,
            String dummyKakaoTemplateJson,
            String dummyRcsBrandJson
    ) {
        this.sessionToken = sessionToken;
        this.dummyCallbackNumber = dummyCallbackNumber;
        this.dummyApiKey = dummyApiKey;
        this.dummyBalance = dummyBalance;
        this.dummySendHistoryJson = dummySendHistoryJson;
        this.dummyKakaoTemplateJson = dummyKakaoTemplateJson;
        this.dummyRcsBrandJson = dummyRcsBrandJson;
    }
}
