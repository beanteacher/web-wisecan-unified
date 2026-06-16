package com.wisecan.unified.domain.dispatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UlidGenerator 단위 테스트.
 */
class UlidGeneratorTest {

    @Test
    @DisplayName("generate() — 길이 26자 ULID 반환")
    void generate_returns26CharString() {
        String ulid = UlidGenerator.generate();
        assertThat(ulid).hasSize(26);
    }

    @Test
    @DisplayName("generate() — Crockford Base32 문자만 포함")
    void generate_onlyCrockfordBase32Chars() {
        String ulid = UlidGenerator.generate();
        assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
    }

    @Test
    @DisplayName("generate() — 연속 호출 시 서로 다른 값 반환 (uniqueness)")
    void generate_returnsUniqueValues() {
        Set<String> ulids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ulids.add(UlidGenerator.generate());
        }
        assertThat(ulids).hasSize(1000);
    }

    @Test
    @DisplayName("generate(timestampMs) — 동일 타임스탬프에서 앞 10자 일치")
    void generate_sameTimestampSamePrefix() {
        long ts = 1_700_000_000_000L;
        String u1 = UlidGenerator.generate(ts);
        String u2 = UlidGenerator.generate(ts);

        // 앞 10자(타임스탬프 부분)는 동일
        assertThat(u1.substring(0, 10)).isEqualTo(u2.substring(0, 10));
        // 뒤 16자(랜덤 부분)는 다름 (확률상 거의 확실)
        assertThat(u1.substring(10)).isNotEqualTo(u2.substring(10));
    }

    @Test
    @DisplayName("generate() — 사전 순 정렬 시 시간 순서와 일치")
    void generate_lexicographicOrderMatchesTimeOrder() {
        // 더 이른 타임스탬프 ULID가 더 늦은 타임스탬프 ULID보다 사전 순으로 앞
        String earlier = UlidGenerator.generate(1_000_000_000_000L);
        String later   = UlidGenerator.generate(1_000_000_000_001L);

        assertThat(earlier.compareTo(later)).isLessThan(0);
    }
}
