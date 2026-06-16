package com.wisecan.unified.domain.template;

import java.util.List;
import java.util.Optional;

/**
 * RCS 템플릿·브랜드 외부 시스템 Adapter 인터페이스.
 *
 * 외부 발송 시스템의 rcs_template / rcs_template_form / rcs_template_component 테이블을 조회한다.
 * 05_DATA_MODEL §6.2, §6.3 참조.
 *
 * 라우팅 정보(agency_id 등)는 회원에게 절대 노출하지 않는다 (INV-02).
 * 구현체는 별도 DataSource(외부 발송 DB)를 사용한다.
 */
public interface RcsTemplateAdapter {

    /**
     * 특정 브랜드에 속한 RCS 템플릿 목록 조회.
     *
     * @param memberId 회원 ID (브랜드 소유권 검증용)
     * @param brandId  RCS 브랜드 ID
     * @return 템플릿 정보 목록 (라우팅 정보 제외)
     */
    List<RcsTemplateInfo> listByBrand(Long memberId, String brandId);

    /**
     * RCS 템플릿 단건 조회.
     *
     * @param memberId      회원 ID (소유권 검증용)
     * @param messagebaseId RCS 템플릿 ID
     * @return 템플릿 정보 (없으면 empty)
     */
    Optional<RcsTemplateInfo> findById(Long memberId, String messagebaseId);

    /**
     * 발송 검증 게이트용 승인 상태 확인.
     * Redis 캐시 miss 시 호출되는 경량 조회.
     *
     * @param memberId      회원 ID
     * @param messagebaseId RCS 템플릿 ID
     * @return true = 승인 + ready 상태의 발송 가능 템플릿
     */
    boolean isApproved(Long memberId, String messagebaseId);

    /**
     * 회원이 등록한 RCS 브랜드 목록 조회.
     *
     * @param memberId 회원 ID
     * @return 브랜드 ID 목록
     */
    List<String> listBrandIds(Long memberId);
}
