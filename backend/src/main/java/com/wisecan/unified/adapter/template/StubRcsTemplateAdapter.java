package com.wisecan.unified.adapter.template;

import com.wisecan.unified.domain.template.RcsApprovalResult;
import com.wisecan.unified.domain.template.RcsTemplateAdapter;
import com.wisecan.unified.domain.template.RcsTemplateInfo;
import com.wisecan.unified.domain.template.RcsTemplateUsageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * RCS 템플릿 Adapter 스텁 구현체.
 *
 * 외부 발송 DB DataSource 가 구성되지 않은 환경(local/test)에서 동작한다.
 * application.yml 의 wisecan.adapter.rcs.stub=true 일 때 활성화된다.
 *
 * 실제 외부 DB 연동 구현체는 외부 발송 시스템 합의 후 별도 구현한다.
 */
@Component
@ConditionalOnProperty(name = "wisecan.adapter.rcs.stub", havingValue = "true", matchIfMissing = true)
@Slf4j
public class StubRcsTemplateAdapter implements RcsTemplateAdapter {

    private static final String STUB_BRAND_ID = "BR.stub_brand_001";

    @Override
    public List<RcsTemplateInfo> listByBrand(Long memberId, String brandId) {
        log.debug("[Stub] RcsTemplateAdapter.listByBrand(memberId={}, brandId={})", memberId, brandId);
        if (!STUB_BRAND_ID.equals(brandId)) {
            return List.of();
        }
        return List.of(
                new RcsTemplateInfo(
                        "rcs_tmpl_test_001",
                        "주문 확인 RCS",
                        STUB_BRAND_ID,
                        RcsTemplateUsageStatus.READY,
                        RcsApprovalResult.승인,
                        null,
                        "tmplt",
                        "RICHCARD",
                        "standalone",
                        "주문하신 상품이 접수되었습니다."
                )
        );
    }

    @Override
    public Optional<RcsTemplateInfo> findById(Long memberId, String messagebaseId) {
        log.debug("[Stub] RcsTemplateAdapter.findById(memberId={}, messagebaseId={})", memberId, messagebaseId);
        if ("rcs_tmpl_test_001".equals(messagebaseId)) {
            return Optional.of(new RcsTemplateInfo(
                    messagebaseId,
                    "주문 확인 RCS",
                    STUB_BRAND_ID,
                    RcsTemplateUsageStatus.READY,
                    RcsApprovalResult.승인,
                    null,
                    "tmplt",
                    "RICHCARD",
                    "standalone",
                    "주문하신 상품이 접수되었습니다."
            ));
        }
        return Optional.empty();
    }

    @Override
    public boolean isApproved(Long memberId, String messagebaseId) {
        log.debug("[Stub] RcsTemplateAdapter.isApproved(memberId={}, messagebaseId={})", memberId, messagebaseId);
        return "rcs_tmpl_test_001".equals(messagebaseId);
    }

    @Override
    public List<String> listBrandIds(Long memberId) {
        log.debug("[Stub] RcsTemplateAdapter.listBrandIds(memberId={})", memberId);
        return List.of(STUB_BRAND_ID);
    }
}
