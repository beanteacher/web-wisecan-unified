package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.template.KakaoInspectionStatus;
import com.wisecan.unified.domain.template.KakaoTemplateAdapter;
import com.wisecan.unified.domain.template.KakaoTemplateInfo;
import com.wisecan.unified.domain.template.KakaoTemplateRegisterRequest;
import com.wisecan.unified.domain.template.KakaoTemplateStatus;
import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoTemplateService 단위 테스트")
class KakaoTemplateServiceTest {

    @Mock
    private KakaoTemplateAdapter kakaoTemplateAdapter;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private KakaoTemplateService kakaoTemplateService;

    private static final String EMAIL = "test@wisecan.com";
    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        kakaoTemplateService = new KakaoTemplateService(
                kakaoTemplateAdapter, memberRepository, redisTemplate);
    }

    private Member member() {
        return Member.builder()
                .email(EMAIL)
                .password("hashed")
                .name("홍길동")
                .phone("01012345678")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private KakaoTemplateInfo approvedTemplate(String code) {
        return new KakaoTemplateInfo(
                code, "주문 알림", "안녕하세요 #{고객명}님",
                KakaoInspectionStatus.APR, KakaoTemplateStatus.A,
                "BA", "002001", null, false);
    }

    // ── 목록 조회 ────────────────────────────────────────────────

    @Nested
    @DisplayName("list()")
    class ListTest {

        @Test
        @DisplayName("회원의 카카오 템플릿 목록을 반환한다")
        void list_returnsTemplates() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(kakaoTemplateAdapter.listByMember(MEMBER_ID))
                    .willReturn(List.of(approvedTemplate("tmpl_001")));

            List<TemplateDto.KakaoTemplateResponse> result = kakaoTemplateService.list(EMAIL);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).templateCode()).isEqualTo("tmpl_001");
            assertThat(result.get(0).sendable()).isTrue();
        }

        @Test
        @DisplayName("회원이 없으면 EntityNotFoundException")
        void list_memberNotFound_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> kakaoTemplateService.list(EMAIL))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── 단건 조회 ────────────────────────────────────────────────

    @Nested
    @DisplayName("detail()")
    class DetailTest {

        @Test
        @DisplayName("존재하는 템플릿 코드 → 상세 반환")
        void detail_found_returnsResponse() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(kakaoTemplateAdapter.findByCode(MEMBER_ID, "tmpl_001"))
                    .willReturn(Optional.of(approvedTemplate("tmpl_001")));

            TemplateDto.KakaoTemplateResponse result =
                    kakaoTemplateService.detail(EMAIL, "tmpl_001");

            assertThat(result.templateCode()).isEqualTo("tmpl_001");
            assertThat(result.inspectionStatus()).isEqualTo("APR");
        }

        @Test
        @DisplayName("존재하지 않는 템플릿 코드 → EntityNotFoundException")
        void detail_notFound_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(kakaoTemplateAdapter.findByCode(MEMBER_ID, "tmpl_999"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> kakaoTemplateService.detail(EMAIL, "tmpl_999"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("tmpl_999");
        }
    }

    // ── 등록 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTest {

        @Test
        @DisplayName("템플릿 등록 성공 → Adapter 반환 코드 전달")
        void register_success_returnsTemplateCode() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(kakaoTemplateAdapter.registerTemplate(eq(MEMBER_ID), any(KakaoTemplateRegisterRequest.class)))
                    .willReturn("tmpl_new_001");

            TemplateDto.KakaoRegisterRequest request = new TemplateDto.KakaoRegisterRequest(
                    "주문 알림", "안녕하세요", "BA", "002", "002001", null, false);

            String code = kakaoTemplateService.register(EMAIL, request);

            assertThat(code).isEqualTo("tmpl_new_001");
        }
    }

    // ── 승인 상태 캐시 ───────────────────────────────────────────

    @Nested
    @DisplayName("isApproved() — Redis 캐시")
    class IsApprovedTest {

        @Test
        @DisplayName("Redis 캐시 HIT('1') → DB 조회 없이 true 반환")
        void isApproved_cacheHit_true_returnsTrue() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("1");

            boolean result = kakaoTemplateService.isApproved(MEMBER_ID, "tmpl_001");

            assertThat(result).isTrue();
            verify(kakaoTemplateAdapter, never()).isApproved(anyLong(), anyString());
        }

        @Test
        @DisplayName("Redis 캐시 HIT('0') → DB 조회 없이 false 반환")
        void isApproved_cacheHit_false_returnsFalse() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("0");

            boolean result = kakaoTemplateService.isApproved(MEMBER_ID, "tmpl_001");

            assertThat(result).isFalse();
            verify(kakaoTemplateAdapter, never()).isApproved(anyLong(), anyString());
        }

        @Test
        @DisplayName("Redis 캐시 MISS → Adapter 조회 후 캐시 갱신")
        void isApproved_cacheMiss_callsAdapterAndCaches() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn(null);
            given(kakaoTemplateAdapter.isApproved(MEMBER_ID, "tmpl_001")).willReturn(true);

            boolean result = kakaoTemplateService.isApproved(MEMBER_ID, "tmpl_001");

            assertThat(result).isTrue();
            verify(kakaoTemplateAdapter).isApproved(MEMBER_ID, "tmpl_001");
            verify(valueOps).set(anyString(), eq("1"), any());
        }

        @Test
        @DisplayName("Redis null(미설정) → Adapter fallback, 캐시 저장 없음")
        void isApproved_redisNull_fallsBackToAdapter() {
            KakaoTemplateService serviceWithoutRedis = new KakaoTemplateService(
                    kakaoTemplateAdapter, memberRepository, null);
            given(kakaoTemplateAdapter.isApproved(MEMBER_ID, "tmpl_001")).willReturn(true);

            boolean result = serviceWithoutRedis.isApproved(MEMBER_ID, "tmpl_001");

            assertThat(result).isTrue();
        }
    }
}
