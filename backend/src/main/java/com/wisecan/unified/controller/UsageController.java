package com.wisecan.unified.controller;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.UsageDto;
import com.wisecan.unified.service.MemberService;
import com.wisecan.unified.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;
    private final MemberService memberService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<UsageDto.Response>>> getHistory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(usageService.getHistory(memberId, page, size)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UsageDto.SummaryResponse>> getSummary() {
        Long memberId = memberService.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.success(usageService.getSummary(memberId)));
    }
}
