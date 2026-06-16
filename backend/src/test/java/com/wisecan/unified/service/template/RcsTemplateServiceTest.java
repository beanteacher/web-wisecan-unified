package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.template.RcsApprovalResult;
import com.wisecan.unified.domain.template.RcsTemplateAdapter;
import com.wisecan.unified.domain.template.RcsTemplateInfo;
import com.wisecan.unified.domain.template.RcsTemplateUsageStatus;
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
@DisplayName("RcsTemplateService 단위 테스트")
class RcsTemplateServiceTest {

    @Mock
    private RcsTemplateAdapter rcsTemplateAdapter;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RcsTemplateService rcsTemplateService;

    private static final String EMAIL = "test@wisecan.com";
    private static final Long MEMBER_ID = 1L;
    private static final String BRAND_ID = "BR.brand_001";

    @BeforeEach
    void setUp() {
        rcsTemplateService = new RcsTemplateService(
                rcsTemplateAdapter, memberRepository, redisTemplate);
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

    private RcsTemplateInfo approvedTemplate(String messagebaseId) {
        return new RcsTemplateInfo(
                messagebaseId, "주문 확인 RCS", BRAND_ID,
                RcsTemplateUsageStatus.READY, RcsApprovalResult.승인,
                null, "tmplt", "RICHCARD", "standalone", "주문이 접수되었습니다.");
    }

    // ── 브랜드 조회 ───────────────────────────────────────────────

    @Nested
    @DisplayName("listBrands()")
    class ListBrandsTest {

        @Test
        @DisplayName("회원의 RCS 브랜드 목록을 반환한다")
        void listBrands_returnsIds() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(rcsTemplateAdapter.listBrandIds(MEMBER_ID)).willReturn(List.of(BRAND_ID));

            List<String> result = rcsTemplateService.listBrands(EMAIL);

            assertThat(result).containsExactly(BRAND_ID);
        }
    }

    // ── 템플릿 목록 ───────────────────────────────────────────────

    @Nested
    @DisplayName("listByBrand()")
    class ListByBrandTest {

        @Test
        @DisplayName("브랜드별 RCS 템플릿 목록 반환")
        void listByBrand_returnsTemplates() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(rcsTemplateAdapter.listByBrand(MEMBER_ID, BRAND_ID))
                    .willReturn(List.of(approvedTemplate("rcs_001")));

            List<TemplateDto.RcsTemplateResponse> result =
                    rcsTemplateService.listByBrand(EMAIL, BRAND_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).messagebaseId()).isEqualTo("rcs_001");
            assertThat(result.get(0).sendable()).isTrue();
        }
    }

    // ── 단건 조회 ────────────────────────────────────────────────

    @Nested
    @DisplayName("detail()")
    class DetailTest {

        @Test
        @DisplayName("존재하는 messagebaseId → 상세 반환")
        void detail_found_returnsResponse() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(rcsTemplateAdapter.findById(MEMBER_ID, "rcs_001"))
                    .willReturn(Optional.of(approvedTemplate("rcs_001")));

            TemplateDto.RcsTemplateResponse result = rcsTemplateService.detail(EMAIL, "rcs_001");

            assertThat(result.messagebaseId()).isEqualTo("rcs_001");
            assertThat(result.approvalResult()).isEqualTo("승인");
        }

        @Test
        @DisplayName("존재하지 않는 messagebaseId → EntityNotFoundException")
        void detail_notFound_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(rcsTemplateAdapter.findById(MEMBER_ID, "rcs_999"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> rcsTemplateService.detail(EMAIL, "rcs_999"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("rcs_999");
        }
    }

    // ── 승인 상태 캐시 ───────────────────────────────────────────

    @Nested
    @DisplayName("isApproved() — Redis 캐시")
    class IsApprovedTest {

        @Test
        @DisplayName("Redis 캐시 HIT('1') → DB 조회 없이 true 반환")
        void isApproved_cacheHit_true() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn("1");

            boolean result = rcsTemplateService.isApproved(MEMBER_ID, "rcs_001");

            assertThat(result).isTrue();
            verify(rcsTemplateAdapter, never()).isApproved(anyLong(), anyString());
        }

        @Test
        @DisplayName("Redis 캐시 MISS → Adapter 조회 후 캐시 갱신")
        void isApproved_cacheMiss_callsAdapterAndCaches() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn(null);
            given(rcsTemplateAdapter.isApproved(MEMBER_ID, "rcs_001")).willReturn(true);

            boolean result = rcsTemplateService.isApproved(MEMBER_ID, "rcs_001");

            assertThat(result).isTrue();
            verify(rcsTemplateAdapter).isApproved(MEMBER_ID, "rcs_001");
            verify(valueOps).set(anyString(), eq("1"), any());
        }

        @Test
        @DisplayName("미승인 템플릿 → false + '0' 캐시")
        void isApproved_notApproved_cachesFalse() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
            given(valueOps.get(anyString())).willReturn(null);
            given(rcsTemplateAdapter.isApproved(MEMBER_ID, "rcs_rej")).willReturn(false);

            boolean result = rcsTemplateService.isApproved(MEMBER_ID, "rcs_rej");

            assertThat(result).isFalse();
            verify(valueOps).set(anyString(), eq("0"), any());
        }
    }
}
