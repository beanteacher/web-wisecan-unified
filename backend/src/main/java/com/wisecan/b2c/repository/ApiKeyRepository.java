package com.wisecan.b2c.repository;

import com.wisecan.b2c.domain.ApiKey;
import com.wisecan.b2c.domain.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByMemberIdAndStatus(Long memberId, ApiKeyStatus status);

    List<ApiKey> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<ApiKey> findByKeyHash(String keyHash);
}
