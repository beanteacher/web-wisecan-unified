package com.wisecan.unified.domain.dispatch;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * ULID(Universally Unique Lexicographically Sortable Identifier) 생성기.
 *
 * <p>발송 요청 단일 식별자({@code send_id})로 사용한다.
 * 외부 라이브러리 없이 표준 Java API만 사용해 구현한다 — 로컬 빌드 SSL 인터셉션 환경 대응.</p>
 *
 * <p>ULID 구조 (26자, Crockford Base32):</p>
 * <pre>
 *  | 10자 (48bit timestamp ms) | 16자 (80bit random) |
 * </pre>
 *
 * <p>스펙 참조: https://github.com/ulid/spec</p>
 */
public final class UlidGenerator {

    /** Crockford Base32 인코딩 문자 셋 */
    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidGenerator() {
        // 유틸리티 클래스 — 인스턴스 생성 금지
    }

    /**
     * 현재 시각 기반 ULID를 생성한다.
     *
     * @return 26자 ULID 문자열 (대문자 Crockford Base32)
     */
    public static String generate() {
        return generate(Instant.now().toEpochMilli());
    }

    /**
     * 지정된 밀리초 타임스탬프 기반 ULID를 생성한다 (테스트 재현성 목적).
     *
     * @param timestampMs 에포크 밀리초
     * @return 26자 ULID 문자열
     */
    static String generate(long timestampMs) {
        char[] ulid = new char[26];

        // 상위 10자 — 48bit 타임스탬프 (ms)
        ulid[0]  = ENCODING[(int) ((timestampMs >>> 45) & 0x1F)];
        ulid[1]  = ENCODING[(int) ((timestampMs >>> 40) & 0x1F)];
        ulid[2]  = ENCODING[(int) ((timestampMs >>> 35) & 0x1F)];
        ulid[3]  = ENCODING[(int) ((timestampMs >>> 30) & 0x1F)];
        ulid[4]  = ENCODING[(int) ((timestampMs >>> 25) & 0x1F)];
        ulid[5]  = ENCODING[(int) ((timestampMs >>> 20) & 0x1F)];
        ulid[6]  = ENCODING[(int) ((timestampMs >>> 15) & 0x1F)];
        ulid[7]  = ENCODING[(int) ((timestampMs >>> 10) & 0x1F)];
        ulid[8]  = ENCODING[(int) ((timestampMs >>>  5) & 0x1F)];
        ulid[9]  = ENCODING[(int) ((timestampMs        ) & 0x1F)];

        // 하위 16자 — 80bit 랜덤
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);

        ulid[10] = ENCODING[(random[0] >>> 3) & 0x1F];
        ulid[11] = ENCODING[((random[0] & 0x07) << 2) | ((random[1] & 0xFF) >>> 6)];
        ulid[12] = ENCODING[(random[1] >>> 1) & 0x1F];
        ulid[13] = ENCODING[((random[1] & 0x01) << 4) | ((random[2] & 0xFF) >>> 4)];
        ulid[14] = ENCODING[((random[2] & 0x0F) << 1) | ((random[3] & 0xFF) >>> 7)];
        ulid[15] = ENCODING[(random[3] >>> 2) & 0x1F];
        ulid[16] = ENCODING[((random[3] & 0x03) << 3) | ((random[4] & 0xFF) >>> 5)];
        ulid[17] = ENCODING[random[4] & 0x1F];
        ulid[18] = ENCODING[(random[5] >>> 3) & 0x1F];
        ulid[19] = ENCODING[((random[5] & 0x07) << 2) | ((random[6] & 0xFF) >>> 6)];
        ulid[20] = ENCODING[(random[6] >>> 1) & 0x1F];
        ulid[21] = ENCODING[((random[6] & 0x01) << 4) | ((random[7] & 0xFF) >>> 4)];
        ulid[22] = ENCODING[((random[7] & 0x0F) << 1) | ((random[8] & 0xFF) >>> 7)];
        ulid[23] = ENCODING[(random[8] >>> 2) & 0x1F];
        ulid[24] = ENCODING[((random[8] & 0x03) << 3) | ((random[9] & 0xFF) >>> 5)];
        ulid[25] = ENCODING[random[9] & 0x1F];

        return new String(ulid);
    }
}
