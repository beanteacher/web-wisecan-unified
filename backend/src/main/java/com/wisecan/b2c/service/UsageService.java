package com.wisecan.b2c.service;

import com.wisecan.b2c.domain.UsageStatus;
import com.wisecan.b2c.dto.UsageDto;
import com.wisecan.b2c.repository.ApiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsageService {

    private final ApiUsageRepository apiUsageRepository;

    public Page<UsageDto.Response> getHistory(Long memberId, int page, int size) {
        return apiUsageRepository
            .findByApiKeyMemberIdOrderByCalledAtDesc(memberId, PageRequest.of(page, size))
            .map(UsageDto.Response::from);
    }

    public UsageDto.SummaryResponse getSummary(Long memberId) {
        long totalCalls = apiUsageRepository.countByApiKeyMemberId(memberId);
        long successCount = apiUsageRepository.countByApiKeyMemberIdAndStatus(memberId, UsageStatus.SUCCESS);
        long failCount = apiUsageRepository.countByApiKeyMemberIdAndStatus(memberId, UsageStatus.FAIL);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayCalls = apiUsageRepository.countByApiKeyMemberIdAndCalledAtAfter(memberId, todayStart);

        return new UsageDto.SummaryResponse(totalCalls, successCount, failCount, todayCalls);
    }
}
