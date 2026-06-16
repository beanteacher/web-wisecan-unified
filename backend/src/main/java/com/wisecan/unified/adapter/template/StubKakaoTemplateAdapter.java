package com.wisecan.unified.adapter.template;

import com.wisecan.unified.domain.template.KakaoInspectionStatus;
import com.wisecan.unified.domain.template.KakaoTemplateAdapter;
import com.wisecan.unified.domain.template.KakaoTemplateInfo;
import com.wisecan.unified.domain.template.KakaoTemplateRegisterRequest;
import com.wisecan.unified.domain.template.KakaoTemplateStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 카카오 템플릿 Adapter 스텁 구현체.
 *
 * 외부 발송 DB DataSource 가 구성되지 않은 환경(local/test)에서 동작한다.
 * application.yml 의 wisecan.adapter.kakao.stub=true 일 때 활성화된다.
 *
 * 실제 외부 DB 연동 구현체는 외부 발송 시스템 합의 후 별도 구현한다.
 */
@Component
@ConditionalOnProperty(name = "wisecan.adapter.kakao.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubKakaoTemplateAdapter implements KakaoTemplateAdapter {

    @Override
    public List<KakaoTemplateInfo> listByMember(Long memberId) {
        log.debug("[Stub] KakaoTemplateAdapter.listByMember(memberId={})", memberId);
        return List.of(
                new KakaoTemplateInfo(
                        "tmpl_test_001",
                        "주문 완료 알림",
                        "안녕하세요 #{고객명}님, 주문이 완료되었습니다.",
                        KakaoInspectionStatus.APR,
                        KakaoTemplateStatus.A,
                        "BA",
                        "002001",
                        null,
                        false
                )
        );
    }

    @Override
    public Optional<KakaoTemplateInfo> findByCode(Long memberId, String templateCode) {
        log.debug("[Stub] KakaoTemplateAdapter.findByCode(memberId={}, templateCode={})", memberId, templateCode);
        if ("tmpl_test_001".equals(templateCode)) {
            return Optional.of(new KakaoTemplateInfo(
                    templateCode,
                    "주문 완료 알림",
                    "안녕하세요 #{고객명}님, 주문이 완료되었습니다.",
                    KakaoInspectionStatus.APR,
                    KakaoTemplateStatus.A,
                    "BA",
                    "002001",
                    null,
                    false
            ));
        }
        return Optional.empty();
    }

    @Override
    public boolean isApproved(Long memberId, String templateCode) {
        log.debug("[Stub] KakaoTemplateAdapter.isApproved(memberId={}, templateCode={})", memberId, templateCode);
        return "tmpl_test_001".equals(templateCode);
    }

    @Override
    public String registerTemplate(Long memberId, KakaoTemplateRegisterRequest request) {
        log.debug("[Stub] KakaoTemplateAdapter.registerTemplate(memberId={}, name={})", memberId, request.templateName());
        return "tmpl_stub_" + System.currentTimeMillis();
    }

    @Override
    public void updateTemplate(Long memberId, String templateCode, KakaoTemplateRegisterRequest request) {
        log.debug("[Stub] KakaoTemplateAdapter.updateTemplate(memberId={}, templateCode={})", memberId, templateCode);
    }

    @Override
    public void deleteTemplate(Long memberId, String templateCode) {
        log.debug("[Stub] KakaoTemplateAdapter.deleteTemplate(memberId={}, templateCode={})", memberId, templateCode);
    }
}
