package com.wisecan.b2c.controller;

import com.wisecan.b2c.domain.Member;
import com.wisecan.b2c.dto.ApiResponse;
import com.wisecan.b2c.dto.UsageDto;
import com.wisecan.b2c.exception.EntityNotFoundException;
import com.wisecan.b2c.repository.MemberRepository;
import com.wisecan.b2c.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;
    private final MemberRepository memberRepository;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<UsageDto.Response>>> getHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Long memberId = resolveCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(usageService.getHistory(memberId, page, size)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UsageDto.SummaryResponse>> getSummary() {
        Long memberId = resolveCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(usageService.getSummary(memberId)));
    }

    private Long resolveCurrentMemberId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Member", 0L));
        return member.getId();
    }
}
