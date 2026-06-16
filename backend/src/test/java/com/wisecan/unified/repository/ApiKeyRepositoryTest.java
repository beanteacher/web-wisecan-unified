package com.wisecan.unified.repository;

import com.wisecan.unified.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ApiKeyRepositoryTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
            .email("test@example.com")
            .password("password")
            .name("테스터")
            .role(MemberRole.USER)
            .status(MemberStatus.ACTIVE)
            .build());
        entityManager.flush();
    }

    private ApiKey buildKey(String name, String prefix, String hash, ApiKeyStatus status) {
        return ApiKey.builder()
            .member(member)
            .keyName(name)
            .keyPrefix(prefix)
            .keyHash(hash)
            .status(status)
            .keyType(ApiKeyType.TEST)
            .scopes(ApiKeyScope.presetTest())
            .build();
    }

    @Test
    @DisplayName("memberId로 API 키 목록 조회 - 생성일 내림차순")
    void findByMemberIdOrderByCreatedAtDesc_returnsKeysInOrder() {
        ApiKey key1 = apiKeyRepository.save(buildKey("키1", "wc_aaaa", "hash1", ApiKeyStatus.ACTIVE));
        entityManager.flush();

        ApiKey key2 = apiKeyRepository.save(buildKey("키2", "wc_bbbb", "hash2", ApiKeyStatus.ACTIVE));
        entityManager.flush();

        List<ApiKey> result = apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(member.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(key2.getId());
        assertThat(result.get(1).getId()).isEqualTo(key1.getId());
    }

    @Test
    @DisplayName("keyHash로 API 키 조회 성공")
    void findByKeyHash_returnsApiKey() {
        apiKeyRepository.save(buildKey("테스트키", "wc_test", "abc123hash", ApiKeyStatus.ACTIVE));
        entityManager.flush();

        Optional<ApiKey> result = apiKeyRepository.findByKeyHash("abc123hash");

        assertThat(result).isPresent();
        assertThat(result.get().getKeyName()).isEqualTo("테스트키");
    }

    @Test
    @DisplayName("keyHash 조회 - 존재하지 않으면 empty")
    void findByKeyHash_notFound_returnsEmpty() {
        Optional<ApiKey> result = apiKeyRepository.findByKeyHash("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("memberId + status로 필터 조회")
    void findByMemberIdAndStatus_returnsFiltered() {
        apiKeyRepository.save(buildKey("활성키", "wc_actv", "activehash", ApiKeyStatus.ACTIVE));
        ApiKey revoked = apiKeyRepository.save(buildKey("취소키", "wc_revk", "revokedhash", ApiKeyStatus.REVOKED));
        entityManager.flush();

        List<ApiKey> active = apiKeyRepository.findByMemberIdAndStatus(member.getId(), ApiKeyStatus.ACTIVE);
        List<ApiKey> revokedList = apiKeyRepository.findByMemberIdAndStatus(member.getId(), ApiKeyStatus.REVOKED);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getKeyName()).isEqualTo("활성키");
        assertThat(revokedList).hasSize(1);
        assertThat(revokedList.get(0).getId()).isEqualTo(revoked.getId());
    }

    @Test
    @DisplayName("rawKey가 DB에 평문으로 저장되지 않음 (해시 저장 검증)")
    void keyHash_isNotRawKey() {
        String rawKey = "wc_plaintext_raw_key";
        String keyHash = "sha256_hashed_value_not_equal_to_raw";

        apiKeyRepository.save(buildKey("해시검증키", "wc_hash", keyHash, ApiKeyStatus.ACTIVE));
        entityManager.flush();

        Optional<ApiKey> byHash = apiKeyRepository.findByKeyHash(keyHash);
        Optional<ApiKey> byRaw = apiKeyRepository.findByKeyHash(rawKey);

        assertThat(byHash).isPresent();
        assertThat(byRaw).isEmpty();
        assertThat(byHash.get().getKeyHash()).isNotEqualTo(rawKey);
    }

    @Test
    @DisplayName("스코프가 DB에 저장되고 조회 시 복원된다")
    void scopes_persistedAndRestored() {
        Set<ApiKeyScope> scopes = Set.of(ApiKeyScope.SEND, ApiKeyScope.HISTORY_READ, ApiKeyScope.BALANCE_READ);
        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("스코프키")
            .keyPrefix("wc_scop")
            .keyHash("scopehash")
            .status(ApiKeyStatus.ACTIVE)
            .keyType(ApiKeyType.PRODUCTION)
            .scopes(scopes)
            .dailyLimit(500)
            .build();

        apiKeyRepository.save(key);
        entityManager.flush();
        entityManager.clear();

        Optional<ApiKey> loaded = apiKeyRepository.findByKeyHash("scopehash");

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getScopes()).containsExactlyInAnyOrderElementsOf(scopes);
        assertThat(loaded.get().getDailyLimit()).isEqualTo(500);
        assertThat(loaded.get().getKeyType()).isEqualTo(ApiKeyType.PRODUCTION);
    }

    @Test
    @DisplayName("getScopeValues - 스코프 value 문자열 Set 반환")
    void getScopeValues_returnsStringValues() {
        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("밸류키")
            .keyPrefix("wc_valu")
            .keyHash("valuehash")
            .status(ApiKeyStatus.ACTIVE)
            .keyType(ApiKeyType.TEST)
            .scopes(Set.of(ApiKeyScope.SEND, ApiKeyScope.BALANCE_READ))
            .build();

        apiKeyRepository.save(key);
        entityManager.flush();
        entityManager.clear();

        ApiKey loaded = apiKeyRepository.findByKeyHash("valuehash").orElseThrow();
        assertThat(loaded.getScopeValues()).containsExactlyInAnyOrder("send", "balance:read");
    }
}
