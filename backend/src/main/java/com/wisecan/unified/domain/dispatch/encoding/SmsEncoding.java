package com.wisecan.unified.domain.dispatch.encoding;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * SMS/LMS/MMS 본문 인코딩 유틸리티.
 *
 * <p>국내 통신사 발송 연동 규격은 EUC-KR(CP949) 인코딩 바이트 스트림을 요구한다.
 * 이 클래스는 다음 두 가지 책임을 가진다.</p>
 *
 * <ol>
 *   <li>UTF-8 문자열을 EUC-KR(CP949) 바이트 배열로 변환 — 변환 불가 문자 처리 포함</li>
 *   <li>EUC-KR 기준 바이트 길이 계산 및 SMS/LMS 타입 분기</li>
 * </ol>
 *
 * <p><b>변환 불가 문자 처리 정책</b><br>
 * EUC-KR(CP949)로 표현할 수 없는 문자(이모지, 특수 유니코드 등)가 포함된 경우
 * {@link #encode(String)} 는 {@link SmsEncodingException}을 던진다.
 * 무손실 변환을 보장하지 못하는 대체 문자(?) 삽입 방식은 사용하지 않는다.
 * 호출자가 사전에 {@link #isEncodable(String)}으로 변환 가능 여부를 확인할 수 있다.</p>
 *
 * <p>02_FEATURE_SPEC.md §6.1 메시지 타입 분기 기준.</p>
 */
public final class SmsEncoding {

    /** EUC-KR 과 MS 확장(CP949, 조합형 한글 포함) 을 모두 포괄하는 Charset */
    static final Charset EUC_KR = Charset.forName("EUC-KR");

    private SmsEncoding() {
        // 유틸리티 클래스 — 인스턴스 생성 금지
    }

    // ── 인코딩 ─────────────────────────────────────────────────────────────

    /**
     * UTF-8 문자열을 EUC-KR(CP949) 바이트 배열로 변환한다.
     *
     * <p>변환 불가 문자가 포함된 경우 {@link SmsEncodingException}을 던진다.
     * 무손실 변환이 보장된 경우에만 바이트 배열을 반환한다.</p>
     *
     * @param text 변환할 UTF-8 문자열 (null 불가)
     * @return EUC-KR 인코딩 바이트 배열
     * @throws SmsEncodingException     변환 불가 문자 포함 시
     * @throws IllegalArgumentException text 가 null 인 경우
     */
    public static byte[] encode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }

        if (!isEncodable(text)) {
            String unmappable = findFirstUnmappableChar(text);
            throw new SmsEncodingException(
                    "EUC-KR 변환 불가 문자 포함: '" + unmappable + "' — EUC-KR 지원 범위를 벗어난 문자(이모지·특수 유니코드 등)는 발송할 수 없습니다."
            );
        }

        return text.getBytes(EUC_KR);
    }

    /**
     * UTF-8 문자열의 EUC-KR 기준 바이트 길이를 반환한다.
     *
     * <p>변환 불가 문자가 포함된 경우 {@link SmsEncodingException}을 던진다.</p>
     *
     * @param text 측정할 UTF-8 문자열 (null 불가)
     * @return EUC-KR 인코딩 시 바이트 수
     * @throws SmsEncodingException     변환 불가 문자 포함 시
     * @throws IllegalArgumentException text 가 null 인 경우
     */
    public static int byteLength(String text) {
        return encode(text).length;
    }

    // ── 타입 분기 ──────────────────────────────────────────────────────────

    /**
     * 본문 길이 기준으로 SMS/LMS 타입을 결정한다.
     *
     * <ul>
     *   <li>EUC-KR 기준 90 byte 이하 → SMS</li>
     *   <li>EUC-KR 기준 91~2,000 byte → LMS</li>
     * </ul>
     *
     * <p>미디어 첨부가 있는 경우 호출자가 MMS를 직접 지정해야 한다.
     * 이 메서드는 본문 길이만으로 분기하며 첨부 여부는 판단하지 않는다.</p>
     *
     * @param text 본문 (null 불가)
     * @return {@link SmsMessageType#SMS} 또는 {@link SmsMessageType#LMS}
     * @throws SmsEncodingException      변환 불가 문자 포함 시
     * @throws SmsEncodingException      2,000 byte 초과 시
     * @throws IllegalArgumentException  text 가 null 인 경우
     */
    public static SmsMessageType resolveType(String text) {
        int len = byteLength(text);

        if (len > SmsMessageType.LMS_MAX_BYTES) {
            throw new SmsEncodingException(
                    "메시지 본문이 LMS 최대 길이(" + SmsMessageType.LMS_MAX_BYTES + " byte)를 초과합니다. "
                    + "현재 길이: " + len + " byte."
            );
        }

        return len <= SmsMessageType.SMS_MAX_BYTES
                ? SmsMessageType.SMS
                : SmsMessageType.LMS;
    }

    // ── 검사 ───────────────────────────────────────────────────────────────

    /**
     * 문자열 전체가 EUC-KR(CP949)로 표현 가능한지 확인한다.
     *
     * @param text 검사할 문자열 (null 불가)
     * @return 모든 문자가 EUC-KR 범위 내이면 true
     */
    public static boolean isEncodable(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        CharsetEncoder encoder = EUC_KR.newEncoder();
        return encoder.canEncode(text);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    /**
     * EUC-KR 변환 불가 첫 번째 문자를 찾아 문자열(코드포인트 표기 포함)로 반환한다.
     * 오류 메시지 작성용으로만 사용한다.
     */
    private static String findFirstUnmappableChar(String text) {
        CharsetEncoder encoder = EUC_KR.newEncoder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            if (!encoder.canEncode(ch)) {
                return ch + "(U+" + String.format("%04X", cp) + ")";
            }
            i += Character.charCount(cp);
        }
        return "(알 수 없음)";
    }
}
