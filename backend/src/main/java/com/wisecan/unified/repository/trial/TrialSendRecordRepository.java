package com.wisecan.unified.repository.trial;

import com.wisecan.unified.domain.trial.TrialSendRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 체험 가상 발송 기록 리포지토리 (W-406).
 */
public interface TrialSendRecordRepository extends JpaRepository<TrialSendRecord, Long> {

    List<TrialSendRecord> findBySessionToken(String sessionToken);

    long countBySessionToken(String sessionToken);
}
