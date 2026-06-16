package com.wisecan.unified.domain.dispatch.gate;

import com.wisecan.unified.domain.dispatch.SendErrorCode;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.SendValidationGate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 검증 5 — 스팸 키워드 필터.
 * 메시지 본문에 금지 키워드가 포함된 경우 발송을 차단한다.
 * 02_FEATURE_SPEC.md §13.1, RQ-SEC-002·003 참조.
 *
 * 운영 환경에서는 외부 스팸 필터 API 또는 DB 기반 키워드 테이블로 교체한다.
 * MVP 단계에서는 하드코딩된 기본 키워드 목록으로 검증한다.
 */
@Component
public class SpamKeywordGate implements SendValidationGate {

    /**
     * MVP 스팸 키워드 기본 목록.
     * 실제 운영에서는 DB 테이블 또는 외부 API로 교체한다.
     */
    private static final List<String> SPAM_KEYWORDS = List.of(
            "대출", "무료대출", "신용불량", "작업대출",
            "불법", "도박", "성인", "19금",
            "스팸", "바이러스", "해킹", "피싱",
            "무기", "마약", "투자보장", "원금보장"
    );

    @Override
    public void validate(SendValidationContext ctx) {
        if (ctx.messageBody() == null || ctx.messageBody().isBlank()) {
            return;
        }

        String body = ctx.messageBody().toLowerCase(Locale.KOREAN);
        for (String keyword : SPAM_KEYWORDS) {
            if (body.contains(keyword.toLowerCase(Locale.KOREAN))) {
                throw new SendValidationException(SendErrorCode.SPAM_KEYWORD_DETECTED,
                        "메시지에 금지 키워드 '" + keyword + "'가 포함되어 있습니다.");
            }
        }
    }

    @Override
    public int order() {
        return 50;
    }
}
