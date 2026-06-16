package com.wisecan.unified.domain.sendernumber;

/**
 * 발신번호 등록 케이스 4종.
 *
 * SELF_MOBILE  — 본인 휴대폰: 본인인증 일치 시 즉시 REGISTERED
 * SELF_LANDLINE — 개인 비-휴대폰: 통신사 명의 확인 추가 인증 후 REGISTERED
 * EMPLOYEE     — 임직원 번호: 재직증명서 + 통신서비스이용증명서 업로드 + 운영자 심사
 * CORP_REP     — 법인 대표번호: 법인 명의 증빙 + 운영자 심사
 */
public enum CallbackRegisterType {
    SELF_MOBILE,
    SELF_LANDLINE,
    EMPLOYEE,
    CORP_REP
}
