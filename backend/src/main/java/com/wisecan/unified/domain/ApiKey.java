package com.wisecan.unified.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "api_key")
@Getter
@NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 100)
    private String keyName;

    @Column(nullable = false, length = 8)
    private String keyPrefix;

    @Column(nullable = false, length = 255)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiKeyStatus status;

    /** TEST / PRODUCTION 키 구분 (02 §5.1, §5.2) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiKeyType keyType;

    /**
     * 허용 스코프 목록 — 열거형 값을 콤마 구분 문자열로 저장.
     * 예: "SEND,HISTORY_READ,BALANCE_READ"
     */
    @Column(name = "scopes", length = 512)
    private String scopesRaw;

    /**
     * 일일 발송 한도 (null = 무제한).
     * 02 §5.3: "일일 발송 한도" 설정 항목.
     */
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    /**
     * 허용 발신번호 화이트리스트 (콤마 구분, null = 제한 없음).
     * 02 §5.3: "발신번호 화이트리스트" 설정 항목.
     */
    @Column(name = "allowed_callbacks", length = 1024)
    private String allowedCallbacksRaw;

    private LocalDateTime lastUsedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public ApiKey(Member member, String keyName, String keyPrefix, String keyHash,
                  ApiKeyStatus status, ApiKeyType keyType, Set<ApiKeyScope> scopes,
                  Integer dailyLimit, String allowedCallbacksRaw) {
        this.member = member;
        this.keyName = keyName;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.status = status;
        this.keyType = keyType != null ? keyType : ApiKeyType.TEST;
        this.scopesRaw = scopesToRaw(scopes);
        this.dailyLimit = dailyLimit;
        this.allowedCallbacksRaw = allowedCallbacksRaw;
    }

    // ─── 도메인 메서드 ──────────────────────────────────────────

    public void revoke() {
        this.status = ApiKeyStatus.REVOKED;
    }

    /**
     * 운영 키 운영자 승인 — PENDING_REVIEW → ACTIVE (§12.6).
     */
    public void activate() {
        if (this.status != ApiKeyStatus.PENDING_REVIEW) {
            throw new IllegalStateException("PENDING_REVIEW 상태인 키만 활성화할 수 있습니다.");
        }
        this.status = ApiKeyStatus.ACTIVE;
    }

    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 스코프 목록 업데이트 (02 §5.3).
     * REVOKED 키는 스코프 변경 불가.
     */
    public void updateScopes(Set<ApiKeyScope> scopes) {
        if (this.status == ApiKeyStatus.REVOKED) {
            throw new IllegalStateException("폐기된 키의 스코프는 변경할 수 없습니다.");
        }
        this.scopesRaw = scopesToRaw(scopes);
    }

    /** 일일 한도·발신번호 화이트리스트 업데이트 (02 §5.3) */
    public void updateLimits(Integer dailyLimit, String allowedCallbacksRaw) {
        if (this.status == ApiKeyStatus.REVOKED) {
            throw new IllegalStateException("폐기된 키의 설정은 변경할 수 없습니다.");
        }
        this.dailyLimit = dailyLimit;
        this.allowedCallbacksRaw = allowedCallbacksRaw;
    }

    // ─── 스코프 헬퍼 ────────────────────────────────────────────

    /** 스코프 enum Set 반환 */
    public Set<ApiKeyScope> getScopes() {
        if (scopesRaw == null || scopesRaw.isBlank()) {
            return Set.of();
        }
        Set<ApiKeyScope> result = new HashSet<>();
        for (String token : scopesRaw.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(ApiKeyScope.valueOf(trimmed));
            }
        }
        return result;
    }

    /** 스코프 string value Set 반환 (ApiKeyAuthFilter 호환) */
    public Set<String> getScopeValues() {
        return getScopes().stream()
            .map(ApiKeyScope::getValue)
            .collect(Collectors.toSet());
    }

    /** 특정 스코프 허용 여부 확인 */
    public boolean hasScope(ApiKeyScope scope) {
        return getScopes().contains(scope);
    }

    private static String scopesToRaw(Set<ApiKeyScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        return scopes.stream()
            .map(Enum::name)
            .collect(Collectors.joining(","));
    }
}
