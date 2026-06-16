package com.wisecan.unified.domain.dispatch;

import com.wisecan.unified.domain.ApiKeyScope;

/**
 * 발송 채널 5종.
 * 각 채널이 요구하는 최소 스코프를 정의한다 (02_FEATURE_SPEC.md §6.1 권한 참조).
 */
public enum SendChannel {

    SMS(ApiKeyScope.SEND_SMS, "SMS"),
    LMS(ApiKeyScope.SEND_SMS, "LMS"),
    MMS(ApiKeyScope.SEND_SMS, "MMS"),
    KAKAO(ApiKeyScope.SEND_KAKAO, "카카오 알림톡"),
    RCS(ApiKeyScope.SEND_RCS, "RCS");

    /** 이 채널 발송에 필요한 채널별 스코프 */
    private final ApiKeyScope channelScope;
    private final String displayName;

    SendChannel(ApiKeyScope channelScope, String displayName) {
        this.channelScope = channelScope;
        this.displayName = displayName;
    }

    public ApiKeyScope getChannelScope() {
        return channelScope;
    }

    public String getDisplayName() {
        return displayName;
    }
}
