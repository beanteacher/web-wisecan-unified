package com.wisecan.unified.controller.admin;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * 운영자 컨트롤러 공통 인증 헬퍼.
 *
 * ADMIN / SUPER_ADMIN 역할 확인 + operatorId 반환 로직을
 * 단일 컴포넌트로 응집하여 컨트롤러 간 중복을 제거한다.
 *
 * W-501 H-1 리뷰 반영: AdminMemberControlController, AdminRoutingController,
 * AdminBillingController 3개 컨트롤러에서 동일하게 구현하던 resolveAdminId() 통합.
 */
@Component
@RequiredArgsConstructor
public class AdminAuthHelper {

    private final MemberRepository memberRepository;

    /**
     * 인증된 사용자가 ADMIN 또는 SUPER_ADMIN 인지 확인하고 operatorId 를 반환한다.
     *
     * @param userDetails Spring Security 인증 주체
     * @return 운영자 memberId
     * @throws EntityNotFoundException  존재하지 않는 계정
     * @throws IllegalArgumentException 운영자 권한이 없는 경우
     */
    public Long resolveAdminId(UserDetails userDetails) {
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("Member", 0L));
        MemberRole role = member.getRole();
        if (role != MemberRole.ADMIN && role != MemberRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("운영자 권한이 필요합니다.");
        }
        return member.getId();
    }
}
