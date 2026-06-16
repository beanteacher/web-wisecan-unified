package com.wisecan.unified.service;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyScope;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.dto.ApiKeyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final MemberRepository memberRepository;

    // ─── 발급 (02 §5.1) ────────────────────────────────────────

    public ApiKeyDto.CreateResponse create(Long memberId, ApiKeyDto.CreateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member", memberId));

        String rawKey = generateRawKey();
        String keyPrefix = rawKey.substring(0, 8);
        String keyHash = sha256(rawKey);

        ApiKeyType keyType = request.keyType() != null ? request.keyType() : ApiKeyType.TEST;
        Set<ApiKeyScope> scopes = resolveScopes(request.scopes(), keyType);
        String allowedCallbacksRaw = joinCallbacks(request.allowedCallbacks());

        ApiKey apiKey = ApiKey.builder()
            .member(member)
            .keyName(request.keyName())
            .keyPrefix(keyPrefix)
            .keyHash(keyHash)
            .status(ApiKeyStatus.ACTIVE)
            .keyType(keyType)
            .scopes(scopes)
            .dailyLimit(request.dailyLimit())
            .allowedCallbacksRaw(allowedCallbacksRaw)
            .build();

        ApiKey saved = apiKeyRepository.save(apiKey);

        return new ApiKeyDto.CreateResponse(
            saved.getId(),
            saved.getKeyName(),
            saved.getKeyPrefix(),
            rawKey,
            saved.getStatus().name(),
            saved.getKeyType().name(),
            saved.getScopes().stream().map(ApiKeyDto.ScopeInfo::from).collect(Collectors.toList()),
            saved.getDailyLimit(),
            saved.getCreatedAt()
        );
    }

    // ─── 목록 조회 ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ApiKeyDto.Response> getMyKeys(Long memberId) {
        return apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId)
            .stream()
            .map(ApiKeyDto.Response::from)
            .toList();
    }

    // ─── 폐기 (02 §5.4) ────────────────────────────────────────

    public void revoke(Long memberId, Long apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new EntityNotFoundException("ApiKey", apiKeyId));

        assertOwner(apiKey, memberId);

        if (apiKey.getStatus() == ApiKeyStatus.REVOKED) {
            throw new RuntimeException("이미 비활성화된 API 키입니다.");
        }

        apiKey.revoke();
    }

    // ─── 재발급 (02 §5.4: rotate) ──────────────────────────────

    /**
     * 기존 키를 즉시 폐기하고 동일 설정(이름·스코프·한도)으로 새 키를 발급한다.
     * 새 rawKey는 1회만 반환된다.
     */
    public ApiKeyDto.CreateResponse rotate(Long memberId, Long apiKeyId) {
        ApiKey old = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new EntityNotFoundException("ApiKey", apiKeyId));

        assertOwner(old, memberId);

        if (old.getStatus() == ApiKeyStatus.REVOKED) {
            throw new RuntimeException("이미 폐기된 키는 재발급할 수 없습니다.");
        }

        // 기존 키 즉시 폐기
        old.revoke();

        // 동일 설정으로 새 키 발급
        String rawKey = generateRawKey();
        String keyPrefix = rawKey.substring(0, 8);
        String keyHash = sha256(rawKey);

        ApiKey newKey = ApiKey.builder()
            .member(old.getMember())
            .keyName(old.getKeyName())
            .keyPrefix(keyPrefix)
            .keyHash(keyHash)
            .status(ApiKeyStatus.ACTIVE)
            .keyType(old.getKeyType())
            .scopes(old.getScopes())
            .dailyLimit(old.getDailyLimit())
            .allowedCallbacksRaw(old.getAllowedCallbacksRaw())
            .build();

        ApiKey saved = apiKeyRepository.save(newKey);

        return new ApiKeyDto.CreateResponse(
            saved.getId(),
            saved.getKeyName(),
            saved.getKeyPrefix(),
            rawKey,
            saved.getStatus().name(),
            saved.getKeyType().name(),
            saved.getScopes().stream().map(ApiKeyDto.ScopeInfo::from).collect(Collectors.toList()),
            saved.getDailyLimit(),
            saved.getCreatedAt()
        );
    }

    // ─── 스코프·한도 수정 (02 §5.3) ─────────────────────────────

    public ApiKeyDto.Response updateScopes(Long memberId, Long apiKeyId, ApiKeyDto.UpdateScopesRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
            .orElseThrow(() -> new EntityNotFoundException("ApiKey", apiKeyId));

        assertOwner(apiKey, memberId);

        apiKey.updateScopes(request.scopes());
        apiKey.updateLimits(request.dailyLimit(), joinCallbacks(request.allowedCallbacks()));

        return ApiKeyDto.Response.from(apiKey);
    }

    // ─── 스코프 카탈로그 (02 §5.3) ──────────────────────────────

    @Transactional(readOnly = true)
    public ApiKeyDto.ScopeCatalogResponse getScopeCatalog() {
        List<ApiKeyDto.ScopeInfo> scopes = Arrays.stream(ApiKeyScope.values())
            .map(ApiKeyDto.ScopeInfo::from)
            .toList();

        ApiKeyDto.PresetInfo presets = new ApiKeyDto.PresetInfo(
            toValueList(ApiKeyScope.presetTest()),
            toValueList(ApiKeyScope.presetSendOnly()),
            toValueList(ApiKeyScope.presetReadOnly()),
            toValueList(ApiKeyScope.presetFull())
        );

        return new ApiKeyDto.ScopeCatalogResponse(scopes, presets);
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────

    private void assertOwner(ApiKey apiKey, Long memberId) {
        if (!apiKey.getMember().getId().equals(memberId)) {
            throw new RuntimeException("해당 API 키에 대한 권한이 없습니다.");
        }
    }

    private Set<ApiKeyScope> resolveScopes(Set<ApiKeyScope> requested, ApiKeyType keyType) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }
        return keyType == ApiKeyType.PRODUCTION
            ? ApiKeyScope.presetSendOnly()
            : ApiKeyScope.presetTest();
    }

    private String joinCallbacks(List<String> callbacks) {
        if (callbacks == null || callbacks.isEmpty()) {
            return null;
        }
        return callbacks.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(","));
    }

    private List<String> toValueList(Set<ApiKeyScope> scopes) {
        return scopes.stream().map(ApiKeyScope::getValue).sorted().toList();
    }

    private String generateRawKey() {
        return "wc_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
