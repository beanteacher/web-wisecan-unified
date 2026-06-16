package com.wisecan.unified.service.security;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.security.AbuseBlock;
import com.wisecan.unified.domain.security.AbuseRuleType;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.security.AbuseBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbuseDetectionService — 자동 차단·해제 서비스")
class AbuseDetectionServiceTest {

    @Mock
    private AbuseBlockRepository abuseBlockRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AbuseDetectionService service;

    // ─── recordAndBlock ───────────────────────────────────────────

    @Test
    @DisplayName("최초 차단 — AbuseBlock 저장 + 회원 SUSPENDED 전환")
    void recordAndBlock_firstTime_savesBlockAndSuspendsMember() {
        Member member = Member.builder()
                .email("test@example.com").password("pw").name("홍길동")
                .role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).build();

        given(abuseBlockRepository.findActiveByMemberIdAndRuleType(1L, AbuseRuleType.BURST_VOLUME))
                .willReturn(Optional.empty());
        given(abuseBlockRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.recordAndBlock(1L, 10L, AbuseRuleType.BURST_VOLUME,
                "60초 내 1001건 감지", 1001L, 1000L);

        ArgumentCaptor<AbuseBlock> captor = ArgumentCaptor.forClass(AbuseBlock.class);
        then(abuseBlockRepository).should().save(captor.capture());

        AbuseBlock saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(1L);
        assertThat(saved.getApiKeyId()).isEqualTo(10L);
        assertThat(saved.getRuleType()).isEqualTo(AbuseRuleType.BURST_VOLUME);
        assertThat(saved.isAutoBlocked()).isTrue();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    @Test
    @DisplayName("이미 동일 규칙 활성 차단 존재 — 중복 저장 없음")
    void recordAndBlock_alreadyBlocked_skips() {
        AbuseBlock existing = AbuseBlock.builder()
                .memberId(1L).apiKeyId(10L).ruleType(AbuseRuleType.BURST_VOLUME)
                .reason("기존 차단").measuredValue(1001L).thresholdValue(1000L)
                .autoBlocked(true).build();

        given(abuseBlockRepository.findActiveByMemberIdAndRuleType(1L, AbuseRuleType.BURST_VOLUME))
                .willReturn(Optional.of(existing));

        service.recordAndBlock(1L, 10L, AbuseRuleType.BURST_VOLUME, "중복 요청", 1002L, 1000L);

        then(abuseBlockRepository).should(never()).save(any());
        then(memberRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("회원이 이미 SUSPENDED — suspend() 미호출, 저장만 수행")
    void recordAndBlock_alreadySuspended_onlySavesBlock() {
        Member member = Member.builder()
                .email("test@example.com").password("pw").name("홍길동")
                .role(MemberRole.MEMBER).status(MemberStatus.SUSPENDED).build();

        given(abuseBlockRepository.findActiveByMemberIdAndRuleType(1L, AbuseRuleType.PATTERN_REPEAT))
                .willReturn(Optional.empty());
        given(abuseBlockRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.recordAndBlock(1L, 10L, AbuseRuleType.PATTERN_REPEAT, "패턴 반복", 6L, 5L);

        then(abuseBlockRepository).should().save(any());
        // SUSPENDED 상태이므로 status 변경 없이 로그만
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    // ─── isBlocked ────────────────────────────────────────────────

    @Test
    @DisplayName("활성 차단 존재 — isBlocked true")
    void isBlocked_activeBlockExists_returnsTrue() {
        given(abuseBlockRepository.existsActiveByMemberId(1L)).willReturn(true);
        assertThat(service.isBlocked(1L)).isTrue();
    }

    @Test
    @DisplayName("활성 차단 없음 — isBlocked false")
    void isBlocked_noActiveBlock_returnsFalse() {
        given(abuseBlockRepository.existsActiveByMemberId(1L)).willReturn(false);
        assertThat(service.isBlocked(1L)).isFalse();
    }

    // ─── unblock ─────────────────────────────────────────────────

    @Test
    @DisplayName("차단 해제 — 다른 활성 차단 없으면 회원 ACTIVE 복구")
    void unblock_noOtherActiveBlock_restoresMemberActive() {
        Member member = Member.builder()
                .email("test@example.com").password("pw").name("홍길동")
                .role(MemberRole.MEMBER).status(MemberStatus.SUSPENDED).build();

        AbuseBlock block = AbuseBlock.builder()
                .memberId(1L).apiKeyId(10L).ruleType(AbuseRuleType.BURST_VOLUME)
                .reason("차단").measuredValue(1001L).thresholdValue(1000L)
                .autoBlocked(true).build();

        given(abuseBlockRepository.findById(99L)).willReturn(Optional.of(block));
        given(abuseBlockRepository.findActiveByMemberId(1L)).willReturn(List.of(block));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.unblock(99L, "정상 확인 후 해제");

        assertThat(block.getUnblockReason()).isEqualTo("정상 확인 후 해제");
        assertThat(block.getUnblockedAt()).isNotNull();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("차단 해제 — 다른 활성 차단 존재하면 회원 SUSPENDED 유지")
    void unblock_otherActiveBlockExists_memberRemasSuspended() {
        Member member = Member.builder()
                .email("test@example.com").password("pw").name("홍길동")
                .role(MemberRole.MEMBER).status(MemberStatus.SUSPENDED).build();

        AbuseBlock block1 = AbuseBlock.builder()
                .memberId(1L).apiKeyId(10L).ruleType(AbuseRuleType.BURST_VOLUME)
                .reason("차단1").measuredValue(1001L).thresholdValue(1000L)
                .autoBlocked(true).build();

        AbuseBlock block2 = AbuseBlock.builder()
                .memberId(1L).apiKeyId(10L).ruleType(AbuseRuleType.PATTERN_REPEAT)
                .reason("차단2").measuredValue(6L).thresholdValue(5L)
                .autoBlocked(true).build();

        given(abuseBlockRepository.findById(99L)).willReturn(Optional.of(block1));
        // block1, block2 모두 활성 상태 반환 (block1은 아직 unblockedAt=null)
        given(abuseBlockRepository.findActiveByMemberId(1L)).willReturn(List.of(block1, block2));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.unblock(99L, "부분 해제");

        // block2가 여전히 활성이므로 SUSPENDED 유지
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    @Test
    @DisplayName("존재하지 않는 blockId — IllegalArgumentException")
    void unblock_notFound_throws() {
        given(abuseBlockRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.unblock(999L, "사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
