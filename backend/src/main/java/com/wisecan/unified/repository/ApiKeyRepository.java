package com.wisecan.unified.repository;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByMemberIdAndStatus(Long memberId, ApiKeyStatus status);

    List<ApiKey> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    Optional<ApiKey> findByKeyHash(String keyHash);

    /** 운영자 검토 큐: PENDING_REVIEW 상태 전체 조회 (§12.6) */
    List<ApiKey> findByStatus(ApiKeyStatus status);

    /**
     * 회원의 특정 유형·상태 키 중 가장 최근 발급된 키 1개 반환.
     * W-206: 웹 콘솔 발송 시 대표 PRODUCTION ACTIVE 키 자동 선택에 사용.
     */
    Optional<ApiKey> findFirstByMemberIdAndKeyTypeAndStatusOrderByCreatedAtDesc(
            Long memberId, ApiKeyType keyType, ApiKeyStatus status);
}
