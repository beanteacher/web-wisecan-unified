package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.template.KakaoTemplateAdapter;
import com.wisecan.unified.domain.template.KakaoTemplateInfo;
import com.wisecan.unified.domain.template.KakaoTemplateRegisterRequest;
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
 * 카카오 알림톡/친구톡 템플릿 서비스.
 *
 * - 외부 발송 시스템의 kko_template 테이블은 {@link KakaoTemplateAdapter}를 통해 접근한다.
 * - 발송 hot path 가속을 위해 승인 상태를 Redis 에 TTL 10분으로 캐싱한다.
 * - 중계사 정보(kko_profile_no 등)는 회원에게 노출하지 않는다 (INV-02).
 * 02_FEATURE_SPEC §9.1 참조.
 */
@Service
@Slf4j
public class KakaoTemplateService {

    /** Redis 캐시 키 접두사: tmpl:approved:kakao:{memberId}:{templateCode} */
    private static final String CACHE_KEY_PREFIX = "tmpl:approved:kakao:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final KakaoTemplateAdapter kakaoTemplateAdapter;
    private final MemberRepository memberRepository;

    @Nullable
    private final StringRedisTemplate redisTemplate;

    public KakaoTemplateService(
            KakaoTemplateAdapter kakaoTemplateAdapter,
            MemberRepository memberRepository,
            @Autowired(required = false) @Nullable StringRedisTemplate redisTemplate) {
        this.kakaoTemplateAdapter = kakaoTemplateAdapter;
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    // ── 조회 ─────────────────────────────────────────────────────

    /**
     * 회원의 카카오 템플릿 목록 조회.
     * 외부 발송 DB 를 Adapter 를 통해 조회한다.
     */
    @Transactional(readOnly = true)
    public List<TemplateDto.KakaoTemplateResponse> list(String email) {
        Member member = findMemberByEmail(email);
        List<KakaoTemplateInfo> infos = kakaoTemplateAdapter.listByMember(member.getId());
        return infos.stream()
                .map(TemplateDto.KakaoTemplateResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 카카오 템플릿 단건 조회.
     */
    @Transactional(readOnly = true)
    public TemplateDto.KakaoTemplateResponse detail(String email, String templateCode) {
        Member member = findMemberByEmail(email);
        KakaoTemplateInfo info = kakaoTemplateAdapter.findByCode(member.getId(), templateCode)
                .orElseThrow(() -> new EntityNotFoundException(
                        "카카오 템플릿을 찾을 수 없습니다: " + templateCode));
        return TemplateDto.KakaoTemplateResponse.from(info);
    }

    // ── 등록·수정·삭제 ───────────────────────────────────────────

    /**
     * 카카오 알림톡 템플릿 등록 신청.
     * 외부 발송 시스템에 템플릿을 등록하고 심사 상태(REG)로 초기화한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public String register(String email, TemplateDto.KakaoRegisterRequest request) {
        Member member = findMemberByEmail(email);
        KakaoTemplateRegisterRequest adapterRequest = new KakaoTemplateRegisterRequest(
                request.templateName(),
                request.templateContent(),
                request.messageType(),
                request.categoryCodeM(),
                request.categoryCodeS(),
                request.buttons(),
                request.securityFlag()
        );
        String templateCode = kakaoTemplateAdapter.registerTemplate(member.getId(), adapterRequest);
        log.info("카카오 템플릿 등록 신청: memberId={}, templateCode={}", member.getId(), templateCode);
        return templateCode;
    }

    /**
     * 카카오 템플릿 수정 신청.
     * 수정 시 승인 상태 캐시를 무효화한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String email, String templateCode, TemplateDto.KakaoRegisterRequest request) {
        Member member = findMemberByEmail(email);
        KakaoTemplateRegisterRequest adapterRequest = new KakaoTemplateRegisterRequest(
                request.templateName(),
                request.templateContent(),
                request.messageType(),
                request.categoryCodeM(),
                request.categoryCodeS(),
                request.buttons(),
                request.securityFlag()
        );
        kakaoTemplateAdapter.updateTemplate(member.getId(), templateCode, adapterRequest);
        evictApprovalCache(member.getId(), templateCode);
        log.info("카카오 템플릿 수정 신청: memberId={}, templateCode={}", member.getId(), templateCode);
    }

    /**
     * 카카오 템플릿 삭제.
     * 삭제 시 승인 상태 캐시를 무효화한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String email, String templateCode) {
        Member member = findMemberByEmail(email);
        kakaoTemplateAdapter.deleteTemplate(member.getId(), templateCode);
        evictApprovalCache(member.getId(), templateCode);
        log.info("카카오 템플릿 삭제: memberId={}, templateCode={}", member.getId(), templateCode);
    }

    // ── 발송 검증 게이트 지원 ────────────────────────────────────

    /**
     * 발송 hot path — 카카오 템플릿 승인 여부 확인.
     * Redis 캐시 HIT 시 DB 조회 생략. MISS 시 Adapter 조회 후 캐시 갱신.
     *
     * @param memberId     회원 ID
     * @param templateCode 템플릿 코드
     * @return true = 발송 가능(APR + A 상태)
     */
    public boolean isApproved(Long memberId, String templateCode) {
        String cacheKey = buildCacheKey(memberId, templateCode);

        // Redis 캐시 HIT
        if (redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("[Cache HIT] kakao approved: memberId={}, templateCode={}", memberId, templateCode);
                    return "1".equals(cached);
                }
            } catch (Exception e) {
                log.warn("Redis 조회 실패 (fallback to DB): {}", e.getMessage());
            }
        }

        // Redis MISS → Adapter 조회
        boolean approved = kakaoTemplateAdapter.isApproved(memberId, templateCode);
        log.debug("[Cache MISS] kakao approved: memberId={}, templateCode={}, result={}", memberId, templateCode, approved);

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

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + email));
    }

    private String buildCacheKey(Long memberId, String templateCode) {
        return CACHE_KEY_PREFIX + memberId + ":" + templateCode;
    }

    private void evictApprovalCache(Long memberId, String templateCode) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(buildCacheKey(memberId, templateCode));
        } catch (Exception e) {
            log.warn("Redis 캐시 무효화 실패: {}", e.getMessage());
        }
    }
}
