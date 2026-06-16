package com.wisecan.unified.dto;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiKeyDto {

    /** 발급 요청 (02 §5.1) */
    public record CreateRequest(
        @NotBlank(message = "키 이름은 필수입니다")
        @Size(max = 100, message = "키 이름은 100자 이하여야 합니다")
        String keyName,

        /** null 이면 TEST 키로 발급 */
        ApiKeyType keyType,

        /** null 이면 권장 프리셋(presetTest) 적용 */
        Set<ApiKeyScope> scopes,

        /** 일일 발송 한도, null = 무제한 */
        @Min(value = 1, message = "일일 한도는 1 이상이어야 합니다")
        Integer dailyLimit,

        /** 허용 발신번호 목록, null = 제한 없음 */
        List<String> allowedCallbacks
    ) {
        /** keyName 만 지정하는 단순 생성 (기존 호환) */
        public CreateRequest(String keyName) {
            this(keyName, null, null, null, null);
        }
    }

    /** 스코프·한도 수정 요청 (02 §5.3) */
    public record UpdateScopesRequest(
        Set<ApiKeyScope> scopes,

        @Min(value = 1, message = "일일 한도는 1 이상이어야 합니다")
        Integer dailyLimit,

        List<String> allowedCallbacks
    ) {}

    /** 목록 조회 응답 */
    public record Response(
        Long id,
        String keyName,
        String keyPrefix,
        String status,
        String keyType,
        List<ScopeInfo> scopes,
        Integer dailyLimit,
        List<String> allowedCallbacks,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
    ) {
        public static Response from(ApiKey apiKey) {
            return new Response(
                apiKey.getId(),
                apiKey.getKeyName(),
                apiKey.getKeyPrefix(),
                apiKey.getStatus().name(),
                apiKey.getKeyType().name(),
                apiKey.getScopes().stream().map(ScopeInfo::from).collect(Collectors.toList()),
                apiKey.getDailyLimit(),
                parseCallbacks(apiKey.getAllowedCallbacksRaw()),
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt()
            );
        }
    }

    /** 발급 직후 응답 (rawKey 포함, 1회성) */
    public record CreateResponse(
        Long id,
        String keyName,
        String keyPrefix,
        String rawKey,
        String status,
        String keyType,
        List<ScopeInfo> scopes,
        Integer dailyLimit,
        LocalDateTime createdAt
    ) {}

    /** 스코프 카탈로그 항목 */
    public record ScopeInfo(
        String name,
        String value,
        String description
    ) {
        public static ScopeInfo from(ApiKeyScope scope) {
            return new ScopeInfo(scope.name(), scope.getValue(), scope.getDescription());
        }
    }

    /** 스코프 카탈로그 전체 응답 */
    public record ScopeCatalogResponse(
        List<ScopeInfo> scopes,
        PresetInfo presets
    ) {}

    /** 권장 프리셋 정보 */
    public record PresetInfo(
        List<String> test,
        List<String> sendOnly,
        List<String> readOnly,
        List<String> full
    ) {}

    // ─── 헬퍼 ──────────────────────────────────────────────────

    private static List<String> parseCallbacks(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return List.of(raw.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
