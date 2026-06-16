package com.wisecan.unified.domain.security;

import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.dispatch.NetworkType;
import com.wisecan.unified.domain.dispatch.SendChannel;
import com.wisecan.unified.domain.dispatch.SendValidationContext;
import com.wisecan.unified.domain.dispatch.SendValidationException;
import com.wisecan.unified.domain.dispatch.gate.BurstVolumeGate;
import com.wisecan.unified.domain.dispatch.gate.PatternRepeatGate;
import com.wisecan.unified.domain.dispatch.gate.AnomalousHourGate;
import com.wisecan.unified.domain.dispatch.gate.SpamKeywordGate;
import com.wisecan.unified.service.security.AbuseDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * false-negative ≤ 5% 시뮬레이션 테스트.
 *
 * 라벨 데이터셋:
 *   - 정상 발송 100건 (false-positive 없어야 함)
 *   - 이상 패턴 100건 (false-negative ≤ 5% = 최대 5건까지 미탐지 허용)
 *
 * 탐지 게이트별 독립 검증:
 *   1. SpamKeywordGate  — 스팸 키워드 탐지
 *   2. BurstVolumeGate  — 발송량 급증 탐지 (Redis mock 기반 누적 시뮬레이션)
 *   3. PatternRepeatGate — 패턴 반복 탐지 (Redis mock 기반 카운터 시뮬레이션)
 *   4. AnomalousHourGate — 비정상 시간대 판별 (시각 함수 직접 검증)
 *
 * DoD: false-negative ≤ 5% (W-504)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("W-504 보안·스팸·이상 패턴 — false-negative ≤ 5% 시뮬레이션")
class AbuseDetectionSimulationTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private AbuseDetectionService abuseDetectionService;

    // ─── 라벨 데이터셋 ────────────────────────────────────────────

    /** 정상 메시지 100건 (탐지되면 안 됨 — false-positive) */
    private static final List<String> NORMAL_MESSAGES = List.of(
            "안녕하세요, 이번 달 청구서를 발송드립니다.",
            "주문이 완료되었습니다. 배송 예정일은 내일입니다.",
            "회원님의 예약이 확정되었습니다.",
            "비밀번호 재설정 링크를 발송드립니다.",
            "오늘 이벤트 안내드립니다.",
            "택배가 출발했습니다.",
            "포인트가 적립되었습니다.",
            "결제가 완료되었습니다.",
            "상품이 발송되었습니다.",
            "고객님의 문의가 접수되었습니다.",
            "미팅 일정을 확인해 주세요.",
            "시스템 점검 안내입니다.",
            "계좌이체가 완료되었습니다.",
            "예약 취소가 완료되었습니다.",
            "새 메시지가 도착했습니다.",
            "오늘 날씨 안내입니다.",
            "서비스 이용 감사합니다.",
            "업데이트가 완료되었습니다.",
            "코드 인증 번호는 123456 입니다.",
            "배달이 완료되었습니다.",
            "리뷰를 남겨주세요.",
            "친구 초대 링크입니다.",
            "구독이 갱신되었습니다.",
            "로그인 알림입니다.",
            "장바구니 상품을 확인해 주세요.",
            "출석 체크 완료되었습니다.",
            "등록이 완료되었습니다.",
            "회원 탈퇴 처리 완료.",
            "정기결제가 예정되어 있습니다.",
            "쿠폰이 발급되었습니다.",
            "당첨을 축하드립니다.",
            "오늘 방문해 주셔서 감사합니다.",
            "약관이 업데이트되었습니다.",
            "보안 알림입니다.",
            "앱 업데이트를 확인해 주세요.",
            "친구 요청이 도착했습니다.",
            "영수증을 발송드립니다.",
            "체험판이 만료됩니다.",
            "계정 정보가 변경되었습니다.",
            "새 공지사항이 있습니다.",
            "홈 화면을 확인해 주세요.",
            "이용약관에 동의해 주세요.",
            "인증이 완료되었습니다.",
            "파일 업로드가 완료되었습니다.",
            "게임 아이템이 지급되었습니다.",
            "적립금 사용 가능합니다.",
            "연장 신청이 완료되었습니다.",
            "배송지 변경이 완료되었습니다.",
            "상담원 연결 중입니다.",
            "월별 리포트가 준비되었습니다.",
            "설문조사에 참여해 주세요.",
            "일정이 등록되었습니다.",
            "팀 초대 알림입니다.",
            "프로필이 업데이트되었습니다.",
            "이메일 인증을 완료해 주세요.",
            "휴대폰 인증 번호는 654321 입니다.",
            "거래 내역을 확인해 주세요.",
            "멤버십이 갱신되었습니다.",
            "광고 수신 동의를 확인해 주세요.",
            "서비스 공지 안내입니다.",
            "신규 기능이 추가되었습니다.",
            "백업이 완료되었습니다.",
            "이슈가 해결되었습니다.",
            "수업이 시작됩니다.",
            "진료 예약이 확인되었습니다.",
            "입금이 확인되었습니다.",
            "출금 신청이 접수되었습니다.",
            "신청서가 제출되었습니다.",
            "검토 중입니다.",
            "승인이 완료되었습니다.",
            "거절 안내입니다.",
            "계약서 서명이 완료되었습니다.",
            "할인 쿠폰을 드립니다.",
            "신상품이 출시되었습니다.",
            "재고 부족 알림입니다.",
            "이달의 혜택을 확인해 주세요.",
            "마일리지가 추가되었습니다.",
            "환불이 처리되었습니다.",
            "취소 신청이 접수되었습니다.",
            "배송 지연 안내입니다.",
            "교환 신청이 완료되었습니다.",
            "AS 접수가 완료되었습니다.",
            "견적서를 발송드립니다.",
            "계약 만료 안내입니다.",
            "보험료 납부 안내입니다.",
            "전기세 청구서입니다.",
            "수도세 납부 기한입니다.",
            "통신비 청구 안내입니다.",
            "인터넷 설치 일정 안내입니다.",
            "TV 수신료 안내입니다.",
            "아파트 관리비 안내입니다.",
            "자동이체 예정 안내입니다.",
            "카드 실적 안내입니다.",
            "신용카드 한도 안내입니다.",
            "체크카드 출금 알림입니다.",
            "통장 잔액 안내입니다.",
            "이자 지급 안내입니다.",
            "만기 도래 안내입니다.",
            "펀드 수익률 안내입니다."
    );

    /** 스팸/이상 패턴 메시지 100건 (탐지되어야 함 — 미탐지 ≤ 5건 허용) */
    private static final List<String> ABUSE_MESSAGES = List.of(
            "대출 승인 보장! 지금 바로 신청하세요",
            "무료대출 가능합니다. 즉시 연락주세요",
            "신용불량자도 대출 OK!",
            "작업대출 전문 업체입니다",
            "불법 도박 사이트 안내",
            "도박 필승 전략 공개",
            "성인 콘텐츠 무료 제공",
            "19금 영상 보러오세요",
            "스팸 메시지 테스트",
            "바이러스 백신 무료 설치",
            "해킹 방지 서비스",
            "피싱 주의 사이트 안내",
            "불법 무기 거래",
            "마약 구매 가능",
            "투자보장 100% 수익",
            "원금보장 투자 상품",
            "대출 이자 없음 보장",
            "무료 대출 상담",
            "불법 복제품 판매",
            "성인 사이트 무료 가입",
            "도박 환전 전문",
            "해킹 툴 무료 배포",
            "마약 류 판매",
            "투자 원금보장 상품 안내",
            "신용불량 대출 전문",
            "불법 스팸 메시지",
            "19금 동영상 공유",
            "피싱 사이트 링크 클릭",
            "작업 대출 전문가",
            "도박 사이트 무료 가입",
            "성인 커뮤니티 초대",
            "바이러스 백신 광고",
            "해킹 서비스 제공",
            "마약 주문 가능",
            "투자 보장 수익 공유",
            "원금 보장 펀드",
            "대출 무료 이자",
            "불법 콘텐츠 판매",
            "무기 밀수 거래",
            "성인 방송 초대장",
            "도박 적중 공식",
            "신용불량 즉시 대출",
            "피싱 URL 전파",
            "바이러스 배포",
            "해킹 계정 판매",
            "마약 배달 서비스",
            "투자금 보장 서비스",
            "원금 100% 보장",
            "대출 무담보 가능",
            "불법 다운로드 사이트",
            "성인 웹툰 무료",
            "도박 배당 안내",
            "신용 불량 대출 가능",
            "피싱 이메일 링크",
            "스팸 봇 메시지",
            "바이러스 링크 클릭",
            "해킹 비밀번호 공유",
            "마약 샘플 제공",
            "투자 원금 보장 약속",
            "원금 손실 없는 투자",
            "대출 이자 면제 서비스",
            "불법 총기 판매",
            "성인 영상 공유",
            "도박 족보 안내",
            "무료 대출 승인",
            "피싱 계좌 안내",
            "스팸 광고 메시지",
            "바이러스 감염 주의",
            "해킹 프로그램 공유",
            "마약 관련 상품 안내",
            "투자 수익 보장 확약",
            "원금 보전 펀드 소개",
            "작업 대출 전문 상담",
            "신용불량 무서류 대출",
            "불법 복권 사기",
            "성인 라이브 방송",
            "도박 머니 환전",
            "피싱 스미싱 안내",
            "스팸 문자 발송",
            "바이러스 백신 사기",
            "해킹 개인정보 판매",
            "마약 파는 곳 안내",
            "투자 보장 수익률 공개",
            "원금보장 적금 안내",
            "대출 보증 무료",
            "불법 영상 유통",
            "성인 게임 초대",
            "도박 우승 비법",
            "무료대출 신청 바로가기",
            "피싱 앱 설치 유도",
            "스팸 필터 우회",
            "바이러스 치료 광고",
            "해킹 방어 서비스 사기",
            "마약 사는 법 안내",
            "투자 원금 100% 보전",
            "원금보장 주식 추천",
            "대출 무이자 서비스",
            "불법 약품 판매",
            "성인 전용 채팅",
            "도박 합법화 안내"
    );

    // ─── 1. SpamKeywordGate 시뮬레이션 ───────────────────────────

    @Test
    @DisplayName("[SpamKeyword] 정상 메시지 100건 — false-positive 0건")
    void spamKeyword_normalMessages_noFalsePositive() {
        SpamKeywordGate gate = new SpamKeywordGate();
        int falsePositive = 0;
        for (String msg : NORMAL_MESSAGES) {
            try {
                gate.validate(ctx(msg, 1));
            } catch (SendValidationException e) {
                falsePositive++;
            }
        }
        assertThat(falsePositive)
                .as("정상 메시지 false-positive 건수")
                .isEqualTo(0);
    }

    @Test
    @DisplayName("[SpamKeyword] 스팸 메시지 100건 — false-negative ≤ 5%")
    void spamKeyword_abuseMessages_falseNegativeBelowThreshold() {
        SpamKeywordGate gate = new SpamKeywordGate();
        int detected = 0;
        for (String msg : ABUSE_MESSAGES) {
            try {
                gate.validate(ctx(msg, 1));
            } catch (SendValidationException e) {
                detected++;
            }
        }
        int total = ABUSE_MESSAGES.size();
        int falseNegative = total - detected;
        double falseNegativeRate = (double) falseNegative / total * 100.0;

        assertThat(falseNegativeRate)
                .as("SpamKeywordGate false-negative 비율: %.1f%% (허용 ≤ 5%%)", falseNegativeRate)
                .isLessThanOrEqualTo(5.0);
    }

    // ─── 2. BurstVolumeGate 시뮬레이션 ──────────────────────────

    @Test
    @DisplayName("[BurstVolume] 누적 임계 초과 시나리오 — 탐지 성공")
    void burstVolume_overThreshold_detected() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        // 누적 901건 상태에서 100건 요청 → 1001 > 1000 임계
        given(valueOps.get(anyString())).willReturn("901");

        BurstVolumeGate gate = new BurstVolumeGate(redisTemplate, abuseDetectionService);
        boolean blocked = false;
        try {
            gate.validate(ctx("정상 메시지", 100));
        } catch (SendValidationException e) {
            blocked = true;
        }
        assertThat(blocked).as("BurstVolume 초과 탐지").isTrue();
    }

    @Test
    @DisplayName("[BurstVolume] 임계 이하 — false-positive 없음")
    void burstVolume_belowThreshold_noFalsePositive() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn("0");
        given(valueOps.increment(anyString(), anyLong())).willReturn(10L);

        BurstVolumeGate gate = new BurstVolumeGate(redisTemplate, abuseDetectionService);
        boolean blocked = false;
        try {
            gate.validate(ctx("정상 메시지", 10));
        } catch (SendValidationException e) {
            blocked = true;
        }
        assertThat(blocked).as("정상 발송 false-positive").isFalse();
    }

    // ─── 3. PatternRepeatGate 시뮬레이션 ─────────────────────────

    @Test
    @DisplayName("[PatternRepeat] 동일 메시지 6회 반복 — 탐지 성공")
    void patternRepeat_sixthRepeat_detected() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn("5"); // 이미 5회 전송됨

        PatternRepeatGate gate = new PatternRepeatGate(redisTemplate, abuseDetectionService);
        boolean blocked = false;
        try {
            gate.validate(ctx("동일 스팸 메시지", 1));
        } catch (SendValidationException e) {
            blocked = true;
        }
        assertThat(blocked).as("패턴 반복 탐지").isTrue();
    }

    @Test
    @DisplayName("[PatternRepeat] 5회 이하 반복 — false-positive 없음")
    void patternRepeat_fiveOrLess_noFalsePositive() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(anyString())).willReturn("4"); // 4회 상태에서 1회 추가 = 5회 이하
        given(valueOps.increment(anyString(), anyLong())).willReturn(5L);

        PatternRepeatGate gate = new PatternRepeatGate(redisTemplate, abuseDetectionService);
        boolean blocked = false;
        try {
            gate.validate(ctx("정상 반복 메시지", 1));
        } catch (SendValidationException e) {
            blocked = true;
        }
        assertThat(blocked).as("5회 이하 false-positive").isFalse();
    }

    // ─── 4. AnomalousHour 시뮬레이션 ────────────────────────────

    @Test
    @DisplayName("[AnomalousHour] 새벽 시간대 20건 — 모두 탐지 대상")
    void anomalousHour_allNightSamples_areAnomalous() {
        List<String> nightTimes = List.of(
                "00:00", "00:30", "01:00", "01:30", "02:00",
                "02:30", "03:00", "03:30", "04:00", "04:30",
                "05:00", "05:10", "05:20", "05:30", "05:40",
                "05:50", "05:55", "05:57", "05:58", "05:59"
        );
        long anomalous = nightTimes.stream()
                .filter(t -> AnomalousHourGate.isAnomalousHour(java.time.LocalTime.parse(t)))
                .count();
        assertThat(anomalous).isEqualTo(20);
    }

    @Test
    @DisplayName("[AnomalousHour] 정상 시간대 20건 — 탐지 대상 아님")
    void anomalousHour_normalTimeSamples_notAnomalous() {
        List<String> dayTimes = List.of(
                "06:00", "07:00", "08:00", "09:00", "10:00",
                "11:00", "12:00", "13:00", "14:00", "15:00",
                "16:00", "17:00", "18:00", "19:00", "20:00",
                "21:00", "22:00", "23:00", "23:30", "23:59"
        );
        long anomalous = dayTimes.stream()
                .filter(t -> AnomalousHourGate.isAnomalousHour(java.time.LocalTime.parse(t)))
                .count();
        assertThat(anomalous).isEqualTo(0);
    }

    // ─── 종합 false-negative 검증 ─────────────────────────────────

    @Test
    @DisplayName("[종합] 스팸 키워드 기반 탐지 — false-negative ≤ 5% DoD 최종 검증")
    void combined_falseNegativeRate_belowDoD() {
        SpamKeywordGate spamGate = new SpamKeywordGate();

        List<String> abuseWithKeywords = new ArrayList<>();
        List<String> abuseWithoutKeywords = new ArrayList<>();

        for (String msg : ABUSE_MESSAGES) {
            try {
                spamGate.validate(ctx(msg, 1));
                abuseWithoutKeywords.add(msg); // 미탐지
            } catch (SendValidationException e) {
                abuseWithKeywords.add(msg); // 탐지됨
            }
        }

        int total = ABUSE_MESSAGES.size();
        int falseNegative = abuseWithoutKeywords.size();
        double rate = (double) falseNegative / total * 100.0;

        // DoD 검증: false-negative ≤ 5%
        assertThat(rate)
                .as("종합 false-negative 비율 %.1f%% — 허용 상한 5%%", rate)
                .isLessThanOrEqualTo(5.0);

        // 탐지율 정보 출력 (테스트 리포트용)
        System.out.printf("[W-504 시뮬레이션] 스팸 탐지율: %.1f%% (%d/%d), false-negative: %.1f%% (%d건)%n",
                (double) abuseWithKeywords.size() / total * 100.0,
                abuseWithKeywords.size(), total, rate, falseNegative);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────

    private SendValidationContext ctx(String body, int recipientCount) {
        return new SendValidationContext(1L, 10L, ApiKeyType.PRODUCTION, "01012345678",
                SendChannel.SMS, body, false, recipientCount, 10L, NetworkType.PRODUCTION, null);
    }
}
