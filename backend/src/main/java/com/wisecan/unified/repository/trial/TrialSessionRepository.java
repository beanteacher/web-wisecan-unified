package com.wisecan.unified.repository.trial;

import com.wisecan.unified.domain.trial.TrialSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 체험 세션 리포지토리 (W-406).
 */
public interface TrialSessionRepository extends JpaRepository<TrialSession, String> {

    /**
     * 특정 IP의 최근 N분 내 신규 세션 발급 횟수를 반환한다 (어뷰징 차단용).
     *
     * @param clientIp  클라이언트 IP
     * @param since     기준 시각 (현재 시각 - 임계 간격)
     * @return 해당 IP의 세션 발급 횟수
     */
    @Query("SELECT COUNT(t) FROM TrialSession t WHERE t.clientIp = :clientIp AND t.createdAt >= :since")
    long countByClientIpSince(@Param("clientIp") String clientIp, @Param("since") LocalDateTime since);
}
