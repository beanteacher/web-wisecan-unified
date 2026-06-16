package com.wisecan.unified.domain.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 발송 정합성 검증 파이프라인 오케스트레이터.
 *
 * Spring이 {@link SendValidationGate} 구현체를 모두 주입한다.
 * order() 오름차순으로 게이트를 순차 실행하며, 첫 실패에서 즉시 예외를 던진다.
 *
 * 검증 순서 (order 기준):
 *   10 — CallerRegistrationGate  (발신번호 등록 여부)
 *   20 — ScopePermissionGate     (API Key 스코프 권한)
 *   30 — CallerWhitelistGate     (발신번호 화이트리스트)
 *   40 — DailyLimitGate          (일일 발송 한도)
 *   50 — SpamKeywordGate         (스팸 키워드 필터)
 *   60 — AdDisclosureGate        (광고 의무 표기)
 *   70 — NightAdBlockGate        (야간 광고 차단)
 *   80 — BalanceGate             (잔액 사전 평가)
 *   90 — PostpaidBlockGate       (후불 연체 차단)
 */
@Service
@Slf4j
public class SendValidationService {

    private final List<SendValidationGate> gates;

    public SendValidationService(List<SendValidationGate> gates) {
        this.gates = gates.stream()
                .sorted(Comparator.comparingInt(SendValidationGate::order))
                .toList();
        log.info("[SendValidation] 등록된 게이트 {} 종: {}",
                this.gates.size(),
                this.gates.stream()
                        .map(g -> g.getClass().getSimpleName())
                        .toList());
    }

    /**
     * 9종 검증 게이트를 순서대로 실행한다.
     * 첫 번째 실패 게이트에서 {@link SendValidationException}을 던진다.
     *
     * @param ctx 발송 요청 컨텍스트
     * @throws SendValidationException 검증 실패 시
     */
    public void validate(SendValidationContext ctx) {
        log.debug("[SendValidation] 검증 시작 — memberId={}, apiKeyId={}, channel={}, callback={}",
                ctx.memberId(), ctx.apiKeyId(), ctx.channel(), ctx.callbackNumber());

        for (SendValidationGate gate : gates) {
            gate.validate(ctx);
            log.debug("[SendValidation] {} 통과", gate.getClass().getSimpleName());
        }

        log.debug("[SendValidation] 전 게이트 통과 — 발송 적재 진행 가능");
    }
}
