package com.wisecan.unified.controller.template;

import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.service.template.KakaoTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카카오 알림톡/친구톡 템플릿 REST API.
 * 02_FEATURE_SPEC §9.1 참조.
 */
@RestController
@RequestMapping("/api/v1/templates/kakao")
@RequiredArgsConstructor
public class KakaoTemplateController {

    private final KakaoTemplateService kakaoTemplateService;

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails user) {
        List<TemplateDto.KakaoTemplateResponse> result = kakaoTemplateService.list(user.getUsername());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{templateCode}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable String templateCode) {
        return ResponseEntity.ok(kakaoTemplateService.detail(user.getUsername(), templateCode));
    }

    @PostMapping
    public ResponseEntity<?> register(@AuthenticationPrincipal UserDetails user,
                                      @RequestBody @Valid TemplateDto.KakaoRegisterRequest request) {
        String templateCode = kakaoTemplateService.register(user.getUsername(), request);
        return ResponseEntity.ok(templateCode);
    }

    @PutMapping("/{templateCode}")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable String templateCode,
                                    @RequestBody @Valid TemplateDto.KakaoRegisterRequest request) {
        kakaoTemplateService.update(user.getUsername(), templateCode, request);
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{templateCode}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable String templateCode) {
        kakaoTemplateService.delete(user.getUsername(), templateCode);
        return ResponseEntity.ok(true);
    }
}
