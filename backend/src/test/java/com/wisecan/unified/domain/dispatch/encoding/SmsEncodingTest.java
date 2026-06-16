package com.wisecan.unified.domain.dispatch.encoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SmsEncoding 단위 테스트.
 *
 * <p>로컬 SSL 인터셉션 환경으로 gradle 빌드 불가 — 컴파일 정합 + 로직 검증으로 DoD 충족.
 * EUC-KR charset 은 JDK 기본 내장이므로 외부 다운로드 없이 동작한다.</p>
 */
@DisplayName("SmsEncoding — UTF-8 → EUC-KR 인코딩 변환 유틸")
class SmsEncodingTest {

    // ── isEncodable ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isEncodable — EUC-KR 변환 가능 여부 확인")
    class IsEncodable {

        @Test
        @DisplayName("ASCII 문자만 포함 — true")
        void ascii_encodable() {
            assertThat(SmsEncoding.isEncodable("Hello World 123")).isTrue();
        }

        @Test
        @DisplayName("한글 KS X 1001 범위 문자 — true")
        void korean_ks_encodable() {
            assertThat(SmsEncoding.isEncodable("안녕하세요. 발송 테스트입니다.")).isTrue();
        }

        @Test
        @DisplayName("CP949 확장 한글(예: 똠) — true")
        void cp949_extended_encodable() {
            // CP949(MS EUC-KR)는 KS X 1001 외 8,822자 추가 지원
            assertThat(SmsEncoding.isEncodable("뚝배기 된장찌개")).isTrue();
        }

        @Test
        @DisplayName("이모지 포함 — false")
        void emoji_not_encodable() {
            assertThat(SmsEncoding.isEncodable("안녕하세요 😀")).isFalse();
        }

        @Test
        @DisplayName("수학 연산자 등 특수 유니코드(U+2202 ∂) — false")
        void special_unicode_not_encodable() {
            // ∂ (PARTIAL DIFFERENTIAL, U+2202) 는 EUC-KR 범위 밖
            assertThat(SmsEncoding.isEncodable("미분 기호 ∂ 포함")).isFalse();
        }

        @Test
        @DisplayName("null 전달 — IllegalArgumentException")
        void null_throws_iae() {
            assertThatThrownBy(() -> SmsEncoding.isEncodable(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("빈 문자열 — true")
        void empty_string_encodable() {
            assertThat(SmsEncoding.isEncodable("")).isTrue();
        }
    }

    // ── encode ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("encode — UTF-8 → EUC-KR 바이트 변환")
    class Encode {

        @Test
        @DisplayName("한글 문자열 변환 후 EUC-KR 디코딩 시 원문 복원 — 무손실")
        void korean_lossless_roundtrip() {
            String original = "안녕하세요. 발송 테스트입니다.";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, Charset.forName("EUC-KR"));
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("ASCII 문자열 변환 후 복원 — 무손실")
        void ascii_lossless_roundtrip() {
            String original = "Hello World 1234567890";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, Charset.forName("EUC-KR"));
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("특수 기호 포함 혼합 문자열 — 무손실")
        void mixed_symbols_lossless_roundtrip() {
            String original = "[WiseCan] 인증번호: 123456 (5분 유효)";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, Charset.forName("EUC-KR"));
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("이모지 포함 — SmsEncodingException")
        void emoji_throws_encoding_exception() {
            assertThatThrownBy(() -> SmsEncoding.encode("이모지 😀 포함"))
                    .isInstanceOf(SmsEncodingException.class)
                    .hasMessageContaining("EUC-KR 변환 불가 문자");
        }

        @Test
        @DisplayName("null 전달 — IllegalArgumentException")
        void null_throws_iae() {
            assertThatThrownBy(() -> SmsEncoding.encode(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("빈 문자열 — 빈 바이트 배열 반환")
        void empty_string_returns_empty_bytes() {
            byte[] result = SmsEncoding.encode("");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("CP949 확장 한글 — 무손실 변환")
        void cp949_extended_lossless() {
            String original = "뚝배기 삼겹살 볶음밥";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, Charset.forName("EUC-KR"));
            assertThat(restored).isEqualTo(original);
        }
    }

    // ── byteLength ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("byteLength — EUC-KR 기준 바이트 길이 계산")
    class ByteLength {

        @Test
        @DisplayName("ASCII 1자 — 1 byte")
        void ascii_one_char_is_one_byte() {
            assertThat(SmsEncoding.byteLength("A")).isEqualTo(1);
        }

        @Test
        @DisplayName("한글 1자 — 2 byte (EUC-KR 기준)")
        void korean_one_char_is_two_bytes() {
            assertThat(SmsEncoding.byteLength("가")).isEqualTo(2);
        }

        @Test
        @DisplayName("빈 문자열 — 0 byte")
        void empty_string_is_zero() {
            assertThat(SmsEncoding.byteLength("")).isEqualTo(0);
        }

        @Test
        @DisplayName("ASCII 10자 — 10 byte")
        void ascii_ten_chars() {
            assertThat(SmsEncoding.byteLength("0123456789")).isEqualTo(10);
        }

        @Test
        @DisplayName("한글 5자 — 10 byte")
        void korean_five_chars() {
            assertThat(SmsEncoding.byteLength("안녕하세요")).isEqualTo(10);
        }

        @Test
        @DisplayName("혼합(한글 3자 + ASCII 4자) — 10 byte")
        void mixed_chars_byte_count() {
            // 한글 3자 = 6 byte, ASCII 4자 = 4 byte → 10 byte
            assertThat(SmsEncoding.byteLength("안녕하 123")).isEqualTo(10);
        }

        @Test
        @DisplayName("이모지 포함 — SmsEncodingException")
        void emoji_throws() {
            assertThatThrownBy(() -> SmsEncoding.byteLength("이모지 😀"))
                    .isInstanceOf(SmsEncodingException.class);
        }
    }

    // ── resolveType ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveType — SMS/LMS 타입 분기")
    class ResolveType {

        @Test
        @DisplayName("빈 문자열 — SMS (0 byte)")
        void empty_is_sms() {
            assertThat(SmsEncoding.resolveType("")).isEqualTo(SmsMessageType.SMS);
        }

        @Test
        @DisplayName("한글 45자(90 byte) — SMS 상한 경계 → SMS")
        void exactly_90_bytes_is_sms() {
            // 한글 1자 = 2 byte → 45자 = 90 byte
            String text = "가".repeat(45);
            assertThat(SmsEncoding.byteLength(text)).isEqualTo(90);
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(SmsMessageType.SMS);
        }

        @Test
        @DisplayName("한글 46자(92 byte) — SMS 초과 → LMS")
        void ninety_two_bytes_is_lms() {
            String text = "가".repeat(46);
            assertThat(SmsEncoding.byteLength(text)).isEqualTo(92);
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(SmsMessageType.LMS);
        }

        @Test
        @DisplayName("한글 1자(2 byte) — SMS")
        void two_bytes_is_sms() {
            assertThat(SmsEncoding.resolveType("가")).isEqualTo(SmsMessageType.SMS);
        }

        @Test
        @DisplayName("ASCII 90자(90 byte) — SMS 경계 정확히 → SMS")
        void ascii_90_chars_is_sms() {
            String text = "A".repeat(90);
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(SmsMessageType.SMS);
        }

        @Test
        @DisplayName("ASCII 91자(91 byte) — LMS")
        void ascii_91_chars_is_lms() {
            String text = "A".repeat(91);
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(SmsMessageType.LMS);
        }

        @Test
        @DisplayName("한글 1000자(2000 byte) — LMS 상한 경계 → LMS")
        void exactly_2000_bytes_is_lms() {
            String text = "가".repeat(1000);
            assertThat(SmsEncoding.byteLength(text)).isEqualTo(2000);
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(SmsMessageType.LMS);
        }

        @Test
        @DisplayName("한글 1001자(2002 byte) — LMS 초과 → SmsEncodingException")
        void over_2000_bytes_throws() {
            String text = "가".repeat(1001);
            assertThatThrownBy(() -> SmsEncoding.resolveType(text))
                    .isInstanceOf(SmsEncodingException.class)
                    .hasMessageContaining("LMS 최대 길이");
        }

        @Test
        @DisplayName("이모지 포함 — SmsEncodingException (인코딩 불가)")
        void emoji_throws_encoding_exception() {
            assertThatThrownBy(() -> SmsEncoding.resolveType("안녕 😀"))
                    .isInstanceOf(SmsEncodingException.class)
                    .hasMessageContaining("EUC-KR 변환 불가 문자");
        }

        @ParameterizedTest
        @CsvSource({
            "안녕하세요. 발송 테스트,SMS",
            "A,SMS"
        })
        @DisplayName("짧은 메시지 — SMS 반환")
        void short_messages_are_sms(String text, SmsMessageType expected) {
            assertThat(SmsEncoding.resolveType(text)).isEqualTo(expected);
        }
    }

    // ── 무손실 변환 보장 — 종합 경계 케이스 ────────────────────────────────

    @Nested
    @DisplayName("무손실 변환 보장 — 경계 케이스")
    class LosslessGuarantee {

        @Test
        @DisplayName("SMS 경계(45자 한글) — encode/decode 완벽 복원")
        void sms_boundary_roundtrip() {
            String original = "가".repeat(45);
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, SmsEncoding.EUC_KR);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("LMS 경계(1000자 한글) — encode/decode 완벽 복원")
        void lms_boundary_roundtrip() {
            String original = "나".repeat(1000);
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, SmsEncoding.EUC_KR);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("광고 문자 형식 본문 — 무손실")
        void ad_message_lossless() {
            String original = "(광고) WiseCan 할인 안내\n수신거부: 080-000-0000";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, SmsEncoding.EUC_KR);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("줄바꿈·탭 포함 — 무손실")
        void newline_tab_lossless() {
            String original = "1번 항목\n2번 항목\t탭포함";
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, SmsEncoding.EUC_KR);
            assertThat(restored).isEqualTo(original);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "인증번호는 123456 입니다.",
            "[WiseCan] 결제가 완료되었습니다.",
            "안녕하세요. 테스트 메시지입니다. Hello World!",
            "특수문자: !@#$%^&*()-_=+[]{}|;':\",./<>?"
        })
        @DisplayName("실무 메시지 샘플 — 무손실 변환")
        void real_world_samples_lossless(String original) {
            byte[] encoded = SmsEncoding.encode(original);
            String restored = new String(encoded, SmsEncoding.EUC_KR);
            assertThat(restored).isEqualTo(original);
        }
    }
}
