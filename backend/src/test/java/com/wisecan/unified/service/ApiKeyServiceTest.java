package com.wisecan.unified.service;

import com.wisecan.unified.domain.*;
import com.wisecan.unified.dto.ApiKeyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member stubMember(Long id) {
        Member member = Member.builder()
            .email("test@example.com")
            .password("pw")
            .name("테스터")
            .role(MemberRole.USER)
            .status(MemberStatus.ACTIVE)
            .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private ApiKey stubApiKey(Member member, ApiKeyStatus status) {
        return ApiKey.builder()
            .member(member)
            .keyName("내 키")
            .keyPrefix("wc_xxxxx")
            .keyHash("sha256hashedvalue")
            .status(status)
            .keyType(ApiKeyType.TEST)
            .scopes(ApiKeyScope.presetTest())
            .build();
    }

    // ─── 발급 ──────────────────────────────────────────────────

    @Test
    @DisplayName("API 키 발급 - rawKey는 1회만 반환, DB에는 해시 저장")
    void create_rawKeyReturnedOnce_hashStoredInDb() {
        Member member = stubMember(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(inv -> inv.getArgument(0));

        ApiKeyDto.CreateResponse response = apiKeyService.create(1L, new ApiKeyDto.CreateRequest("내 키"));

        assertThat(response.rawKey()).isNotNull();
        assertThat(response.rawKey()).startsWith("wc_");
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    @DisplayName("API 키 발급 - 스코프 미지정 시 TEST 프리셋 적용")
    void create_noScopes_appliesTestPreset() {
        Member member = stubMember(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(inv -> inv.getArgument(0));

        ApiKeyDto.CreateResponse response = apiKeyService.create(1L, new ApiKeyDto.CreateRequest("테스트 키"));

        // TEST 프리셋: SEND + HISTORY_READ
        assertThat(response.scopes()).isNotEmpty();
        List<String> scopeValues = response.scopes().stream().map(ApiKeyDto.ScopeInfo::value).toList();
        assertThat(scopeValues).contains("send", "history:read");
    }

    @Test
    @DisplayName("API 키 발급 - PRODUCTION 타입 + 스코프 미지정 시 sendOnly 프리셋 적용")
    void create_productionNoScopes_appliesSendOnlyPreset() {
        Member member = stubMember(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(inv -> inv.getArgument(0));

        ApiKeyDto.CreateRequest request = new ApiKeyDto.CreateRequest(
            "운영 키", ApiKeyType.PRODUCTION, null, null, null);

        ApiKeyDto.CreateResponse response = apiKeyService.create(1L, request);

        assertThat(response.keyType()).isEqualTo("PRODUCTION");
        List<String> scopeValues = response.scopes().stream().map(ApiKeyDto.ScopeInfo::value).toList();
        assertThat(scopeValues).contains("send", "history:read");
    }

    @Test
    @DisplayName("API 키 발급 - 존재하지 않는 memberId면 EntityNotFoundException")
    void create_memberNotFound_throwsEntityNotFoundException() {
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.create(99L, new ApiKeyDto.CreateRequest("키")))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ─── 목록 조회 ──────────────────────────────────────────────

    @Test
    @DisplayName("내 API 키 목록 조회")
    void getMyKeys_returnsKeys() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L)).willReturn(List.of(key));

        List<ApiKeyDto.Response> result = apiKeyService.getMyKeys(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).keyName()).isEqualTo("내 키");
        assertThat(result.get(0).status()).isEqualTo("ACTIVE");
    }

    // ─── 폐기 ──────────────────────────────────────────────────

    @Test
    @DisplayName("API 키 revoke - 성공")
    void revoke_success() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        apiKeyService.revoke(1L, 10L);

        assertThat(key.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    @DisplayName("API 키 revoke - 다른 멤버가 시도하면 RuntimeException")
    void revoke_unauthorizedMember_throwsRuntimeException() {
        Member owner = stubMember(1L);
        ApiKey key = stubApiKey(owner, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.revoke(2L, 10L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("API 키 revoke - 이미 REVOKED된 키면 RuntimeException")
    void revoke_alreadyRevoked_throwsRuntimeException() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.REVOKED);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.revoke(1L, 10L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("이미 비활성화");
    }

    // ─── 재발급 ─────────────────────────────────────────────────

    @Test
    @DisplayName("rotate - 기존 키 REVOKED 후 새 키 발급, rawKey 반환")
    void rotate_success_oldRevoked_newKeyReturned() {
        Member member = stubMember(1L);
        ApiKey old = stubApiKey(member, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(old));
        given(apiKeyRepository.save(any(ApiKey.class))).willAnswer(inv -> inv.getArgument(0));

        ApiKeyDto.CreateResponse response = apiKeyService.rotate(1L, 10L);

        assertThat(old.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(response.rawKey()).startsWith("wc_");
        assertThat(response.keyName()).isEqualTo("내 키");
    }

    @Test
    @DisplayName("rotate - 이미 폐기된 키는 재발급 불가")
    void rotate_alreadyRevoked_throwsRuntimeException() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.REVOKED);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.rotate(1L, 10L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("폐기된 키는 재발급");
    }

    @Test
    @DisplayName("rotate - 다른 멤버면 RuntimeException")
    void rotate_unauthorizedMember_throwsRuntimeException() {
        Member owner = stubMember(1L);
        ApiKey key = stubApiKey(owner, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        assertThatThrownBy(() -> apiKeyService.rotate(2L, 10L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("권한");
    }

    // ─── 스코프 수정 ─────────────────────────────────────────────

    @Test
    @DisplayName("updateScopes - 스코프·한도 정상 변경")
    void updateScopes_success() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.ACTIVE);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        Set<ApiKeyScope> newScopes = Set.of(ApiKeyScope.SEND, ApiKeyScope.BALANCE_READ);
        ApiKeyDto.UpdateScopesRequest request = new ApiKeyDto.UpdateScopesRequest(newScopes, 1000, null);

        ApiKeyDto.Response response = apiKeyService.updateScopes(1L, 10L, request);

        assertThat(response.dailyLimit()).isEqualTo(1000);
        assertThat(response.scopes().stream().map(ApiKeyDto.ScopeInfo::value).toList())
            .containsExactlyInAnyOrder("send", "balance:read");
    }

    @Test
    @DisplayName("updateScopes - REVOKED 키는 변경 불가")
    void updateScopes_revokedKey_throwsIllegalStateException() {
        Member member = stubMember(1L);
        ApiKey key = stubApiKey(member, ApiKeyStatus.REVOKED);

        given(apiKeyRepository.findById(10L)).willReturn(Optional.of(key));

        Set<ApiKeyScope> newScopes = Set.of(ApiKeyScope.SEND);
        ApiKeyDto.UpdateScopesRequest request = new ApiKeyDto.UpdateScopesRequest(newScopes, null, null);

        assertThatThrownBy(() -> apiKeyService.updateScopes(1L, 10L, request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("폐기된 키");
    }

    // ─── 스코프 카탈로그 ─────────────────────────────────────────

    @Test
    @DisplayName("getScopeCatalog - 12종 스코프 + 4개 프리셋 반환")
    void getScopeCatalog_returns12ScopesAnd4Presets() {
        ApiKeyDto.ScopeCatalogResponse catalog = apiKeyService.getScopeCatalog();

        assertThat(catalog.scopes()).hasSize(12);
        assertThat(catalog.presets().test()).isNotEmpty();
        assertThat(catalog.presets().sendOnly()).isNotEmpty();
        assertThat(catalog.presets().readOnly()).isNotEmpty();
        assertThat(catalog.presets().full()).hasSize(12);
    }
}
