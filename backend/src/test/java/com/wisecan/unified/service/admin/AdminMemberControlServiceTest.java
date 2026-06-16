package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.admin.MemberControlAuditLog;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.admin.AdminMemberControlDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.admin.MemberControlAuditLogRepository;
import com.wisecan.unified.service.sendernumber.CallbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * AdminMemberControlService 단위 테스트 — §12.3.
 * TDD 순서: RED → GREEN (로컬 빌드 불가 환경이므로 컴파일 정합 검증으로 대체).
 */
@ExtendWith(MockitoExtension.class)
class AdminMemberControlServiceTest {

    @Mock private MemberRepository memberRepository;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private CallbackService callbackService;
    @Mock private MemberControlAuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminMemberControlService adminMemberControlService;

    // ── §12.3 강제 정지 ──────────────────────────────────────────────────

    @Test
    @DisplayName("정상 회원 강제 정지 — ACTIVE → SUSPENDED, 발신번호·키 연쇄 처리")
    void suspend_activeMember_suspendedWithCascade() {
        // given
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        ApiKey activeKey = ApiKey.builder()
                .member(member)
                .keyName("테스트키")
                .keyPrefix("sk_test")
                .keyHash("hash")
                .status(ApiKeyStatus.ACTIVE)
                .keyType(ApiKeyType.TEST)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L))
                .willReturn(List.of(activeKey));
        given(auditLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        // when
        AdminMemberControlDto.MemberStatusResponse response =
                adminMemberControlService.suspend(1L, 99L, "스팸 발송 위반");

        // then
        assertThat(response.status()).isEqualTo(MemberStatus.SUSPENDED);
        assertThat(activeKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        verify(callbackService).deleteAllByMember(1L, 99L);
        verify(auditLogRepository).save(any(MemberControlAuditLog.class));
    }

    @Test
    @DisplayName("이미 정지된 회원 정지 시도 — IllegalStateException")
    void suspend_alreadySuspended_throwsException() {
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.SUSPENDED)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> adminMemberControlService.suspend(1L, 99L, "사유"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 정지된 회원");
    }

    @Test
    @DisplayName("운영자 계정 정지 시도 — IllegalArgumentException")
    void suspend_adminMember_throwsException() {
        Member admin = Member.builder()
                .email("admin@example.com")
                .password("encoded")
                .name("운영자")
                .role(MemberRole.ADMIN)
                .status(MemberStatus.ACTIVE)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminMemberControlService.suspend(1L, 99L, "사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("운영자 계정");
    }

    // ── §12.3 강제 해지 ──────────────────────────────────────────────────

    @Test
    @DisplayName("정상 회원 강제 해지 — ACTIVE → TERMINATED, 연쇄 처리")
    void terminate_activeMember_terminatedWithCascade() {
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(1L))
                .willReturn(List.of());
        given(auditLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminMemberControlDto.MemberStatusResponse response =
                adminMemberControlService.terminate(1L, 99L, "영구 정책 위반");

        assertThat(response.status()).isEqualTo(MemberStatus.TERMINATED);
        verify(callbackService).deleteAllByMember(1L, 99L);
        verify(auditLogRepository).save(any(MemberControlAuditLog.class));
    }

    @Test
    @DisplayName("이미 해지된 회원 해지 시도 — IllegalStateException")
    void terminate_alreadyTerminated_throwsException() {
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.TERMINATED)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> adminMemberControlService.terminate(1L, 99L, "사유"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 해지된 회원");
    }

    // ── §12.3 정지 해제 ──────────────────────────────────────────────────

    @Test
    @DisplayName("정지 해제 — SUSPENDED → ACTIVE")
    void unsuspend_suspendedMember_active() {
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.SUSPENDED)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(auditLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        AdminMemberControlDto.MemberStatusResponse response =
                adminMemberControlService.unsuspend(1L, 99L, "조치 완료");

        assertThat(response.status()).isEqualTo(MemberStatus.ACTIVE);
        verify(auditLogRepository).save(any(MemberControlAuditLog.class));
    }

    @Test
    @DisplayName("ACTIVE 회원 정지 해제 시도 — IllegalStateException")
    void unsuspend_activeMember_throwsException() {
        Member member = Member.builder()
                .email("user@example.com")
                .password("encoded")
                .name("홍길동")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> adminMemberControlService.unsuspend(1L, 99L, "사유"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("정지 상태인 회원만");
    }

    // ── 존재하지 않는 회원 ────────────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 회원 정지 — EntityNotFoundException")
    void suspend_memberNotFound_throwsEntityNotFoundException() {
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminMemberControlService.suspend(999L, 99L, "사유"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
