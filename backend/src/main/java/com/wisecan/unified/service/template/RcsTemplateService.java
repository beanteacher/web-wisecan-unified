package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.template.RcsTemplateAdapter;
import com.wisecan.unified.domain.template.RcsTemplateInfo;
import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RCS 템플릿·브랜드 서비스.
 *
 * - 외부 발송 시스템의 rcs_template 테이블은 {@link RcsTemplateAdapter}를 통해 접근한다.
 * - 발송 hot path 가속을 위해 승인 상태를 Redis 에 TTL 10분으로 캐싱한다.
 * - 라우팅 정보(agency_id 등)는 회원에게 노출하지 않는다 (INV-02).
 * 02_FEATURE_SPEC §9.2 참조.
 */
@Service
@Slf4j
public class RcsTemplateService {

    /** Redis 캐시 키 접두사: tmpl:approved:rcs:{memberId}:{messagebaseId} */
    private static final String CACHE_KEY_PREFIX = "tmpl:approved:rcs:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final RcsTemplateAdapter rcsTemplateAdapter;
    private final MemberRepository memberRepository;

    @Nullable
    private final StringRedisTemplate redisTemplate;

    public RcsTemplateService(
            RcsTemplateAdapter rcsTemplateAdapter,
            MemberRepository memberRepository,
            @Autowired(required = false) @Nullable StringRedisTemplate redisTemplate) {
        this.rcsTemplateAdapter = rcsTemplateAdapter;
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    // ── 브랜드 조회 ───────────────────────────────────────────────

    /**
     * 회원이 보유한 RCS 브랜드 ID 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<String> listBrands(String email) {
        Member member = findMemberByEmail(email);
        return rcsTemplateAdapter.listBrandIds(member.getId());
    }

    // ── 템플릿 조회 ───────────────────────────────────────────────

    /**
     * 특정 브랜드의 RCS 템플릿 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<TemplateDto.RcsTemplateResponse> listByBrand(String email, String brandId) {
        Member member = findMemberByEmail(email);
        List<RcsTemplateInfo> infos = rcsTemplateAdapter.listByBrand(member.getId(), brandId);
        return infos.stream()
                .map(TemplateDto.RcsTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * RCS 템플릿 단건 조회.
     */
    @Transactional(readOnly = true)
    public TemplateDto.RcsTemplateResponse detail(String email, String messagebaseId) {
        Member member = findMemberByEmail(email);
        RcsTemplateInfo info = rcsTemplateAdapter.findById(member.getId(), messagebaseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "RCS 템플릿을 찾을 수 없습니다: " + messagebaseId));
        return TemplateDto.RcsTemplateResponse.from(info);
    }

    // ── 발송 검증 게이트 지원 ────────────────────────────────────

    /**
     * 발송 hot path — RCS 템플릿 승인 여부 확인.
     * Redis 캐시 HIT 시 DB 조회 생략. MISS 시 Adapter 조회 후 캐시 갱신.
     *
     * @param memberId      회원 ID
     * @param messagebaseId RCS 템플릿 ID
     * @return true = 발송 가능(승인 + ready 상태)
     */
    public boolean isApproved(Long memberId, String messagebaseId) {
        String cacheKey = buildCacheKey(memberId, messagebaseId);

        // Redis 캐시 HIT
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("[Cache HIT] rcs approved: memberId={}, messagebaseId={}", memberId, messagebaseId);
                    return "1".equals(cached);
                }
            } catch (Exception e) {
                log.warn("Redis 조회 실패 (fallback to DB): {}", e.getMessage());
            }
        }

        // Redis MISS → Adapter 조회
        boolean approved = rcsTemplateAdapter.isApproved(memberId, messagebaseId);
        log.debug("[Cache MISS] rcs approved: memberId={}, messagebaseId={}, result={}",
                memberId, messagebaseId, approved);

        // 캐시 갱신
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, approved ? "1" : "0", CACHE_TTL);
            } catch (Exception e) {
                log.warn("Redis 캐시 갱신 실패: {}", e.getMessage());
            }
        }

        return approved;
    }

    /**
     * 캐시 무효화 — 외부 시스템에서 템플릿 상태가 변경되었을 때 호출.
     */
    public void evictApprovalCache(Long memberId, String messagebaseId) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(buildCacheKey(memberId, messagebaseId));
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패: {}", e.getMessage());
        }
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + email));
    }

    private String buildCacheKey(Long memberId, String messagebaseId) {
        return CACHE_KEY_PREFIX + memberId + ":" + messagebaseId;
    }
}
