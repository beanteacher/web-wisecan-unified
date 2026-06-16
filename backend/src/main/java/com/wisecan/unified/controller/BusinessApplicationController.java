package com.wisecan.unified.controller;

import com.wisecan.unified.dto.ApiResponse;
import com.wisecan.unified.dto.BusinessApplicationDto;
import com.wisecan.unified.service.BusinessApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/business-applications")
@RequiredArgsConstructor
public class BusinessApplicationController {

    private final BusinessApplicationService businessApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessApplicationDto.StatusResponse>> submit(
        @RequestBody @Valid BusinessApplicationDto.SubmitRequest request,
        Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(businessApplicationService.submit(email, request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<BusinessApplicationDto.StatusResponse>> getStatus(
        Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(businessApplicationService.getStatus(email)));
    }
}
