package com.wisecan.unified.controller.template;

import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.service.template.RcsTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RCS 템플릿·브랜드 REST API.
 * 02_FEATURE_SPEC §9.2 참조.
 */
@RestController
@RequestMapping("/api/v1/templates/rcs")
@RequiredArgsConstructor
public class RcsTemplateController {

    private final RcsTemplateService rcsTemplateService;

    @GetMapping("/brands")
    public ResponseEntity<?> listBrands(@AuthenticationPrincipal UserDetails user) {
        List<String> brands = rcsTemplateService.listBrands(user.getUsername());
        return ResponseEntity.ok(brands);
    }

    @GetMapping("/brands/{brandId}")
    public ResponseEntity<?> listByBrand(@AuthenticationPrincipal UserDetails user,
                                         @PathVariable String brandId) {
        List<TemplateDto.RcsTemplateResponse> result =
                rcsTemplateService.listByBrand(user.getUsername(), brandId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{messagebaseId}")
    public ResponseEntity<?> detail(@AuthenticationPrincipal UserDetails user,
                                    @PathVariable String messagebaseId) {
        return ResponseEntity.ok(rcsTemplateService.detail(user.getUsername(), messagebaseId));
    }
}
