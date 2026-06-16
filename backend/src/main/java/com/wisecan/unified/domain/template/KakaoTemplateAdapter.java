package com.wisecan.unified.domain.template;

import java.util.List;
import java.util.Optional;

/**
 * 카카오 알림톡 템플릿 외부 시스템 Adapter 인터페이스.
 *
 * 외부 발송 시스템의 kko_template / kko_brand_template 테이블을 조회한다.
 * 05_DATA_MODEL §6.1, §6.3 참조.
 *
 * 라우팅 매핑(kko_profile_no 등 중계사 식별자)은 회원에게 절대 노출하지 않는다 (INV-02).
 * 구현체는 별도 DataSource(외부 발송 DB)를 사용한다.
 */
public interface KakaoTemplateAdapter {

    /**
     * 회원의 카카오 프로필(브랜드)에 속한 템플릿 목록 조회.
     *
     * @param memberId 회원 ID (kko_profile 의 소유자 식별에 사용)
     * @return 템플릿 정보 목록 (중계사 정보 제외)
     */
    List<KakaoTemplateInfo> listByMember(Long memberId);

    /**
     * 특정 템플릿 코드로 단건 조회.
     *
     * @param memberId     회원 ID (소유권 검증용)
     * @param templateCode 템플릿 코드
     * @return 템플릿 정보 (없으면 empty)
     */
    Optional<KakaoTemplateInfo> findByCode(Long memberId, String templateCode);

    /**
     * 발송 검증 게이트용 승인 상태 확인.
     * Redis 캐시 miss 시 호출되는 경량 조회.
     *
     * @param memberId     회원 ID
     * @param templateCode 템플릿 코드
     * @return true = 승인된 발송 가능 템플릿
     */
    boolean isApproved(Long memberId, String templateCode);

    /**
     * 카카오 알림톡 템플릿 등록 신청 (중계사 인터페이스 호출).
     * 외부 발송 시스템 API를 통해 카카오에 등록 요청을 전달한다.
     *
     * @param memberId 회원 ID
     * @param request  등록 요청 파라미터
     * @return 등록된 템플릿 코드
     */
    String registerTemplate(Long memberId, KakaoTemplateRegisterRequest request);

    /**
     * 카카오 알림톡 템플릿 수정 신청.
     *
     * @param memberId     회원 ID
     * @param templateCode 수정 대상 코드
     * @param request      수정 요청 파라미터
     */
    void updateTemplate(Long memberId, String templateCode, KakaoTemplateRegisterRequest request);

    /**
     * 카카오 알림톡 템플릿 삭제 신청.
     *
     * @param memberId     회원 ID
     * @param templateCode 삭제 대상 코드
     */
    void deleteTemplate(Long memberId, String templateCode);
}
