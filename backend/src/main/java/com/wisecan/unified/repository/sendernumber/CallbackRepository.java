package com.wisecan.unified.repository.sendernumber;

import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CallbackRepository extends JpaRepository<Callback, Long> {

    /** 회원의 발신번호 목록 (삭제 제외) */
    List<Callback> findByMemberIdAndStatusNot(Long memberId, CallbackStatus status);

    /** 회원의 전체 발신번호 목록 */
    List<Callback> findByMemberId(Long memberId);

    /** 특정 번호의 활성 등록 존재 여부 (활성 1개 강제 검증용) */
    boolean existsByPhoneNumberAndStatusIn(String phoneNumber, List<CallbackStatus> statuses);

    /** 특정 번호의 활성 등록 조회 */
    Optional<Callback> findByPhoneNumberAndStatusIn(String phoneNumber, List<CallbackStatus> statuses);

    /** 운영자 심사 큐: SUBMITTED 또는 UNDER_REVIEW 상태 전체 */
    List<Callback> findByStatusIn(List<CallbackStatus> statuses);

    /** 회원의 등록 완료(REGISTERED) 발신번호 목록 (발송 검증 hot-path) */
    List<Callback> findByMemberIdAndStatus(Long memberId, CallbackStatus status);

    /** 회사의 REGISTERED 발신번호 목록 */
    List<Callback> findByCompanyIdAndStatus(Long companyId, CallbackStatus status);

    /**
     * 발송 검증용 — 회원의 특정 번호가 지정 상태인지 확인 (hot-path).
     * CallerRegistrationGate에서 사용. Redis 캐시 미스 시 이 메서드로 DB 조회.
     */
    boolean existsByMemberIdAndPhoneNumberAndStatus(Long memberId, String phoneNumber, CallbackStatus status);
}
