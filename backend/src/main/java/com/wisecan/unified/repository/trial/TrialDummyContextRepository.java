package com.wisecan.unified.repository.trial;

import com.wisecan.unified.domain.trial.TrialDummyContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 체험 더미 컨텍스트 리포지토리 (W-406).
 */
public interface TrialDummyContextRepository extends JpaRepository<TrialDummyContext, Long> {

    Optional<TrialDummyContext> findBySessionToken(String sessionToken);
}
