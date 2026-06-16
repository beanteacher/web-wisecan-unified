package com.wisecan.unified.repository.sendernumber;

import com.wisecan.unified.domain.sendernumber.CallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CallbackLogRepository extends JpaRepository<CallbackLog, Long> {

    List<CallbackLog> findByCallbackIdOrderByOccurredAtDesc(Long callbackId);
}
