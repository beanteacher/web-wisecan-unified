package com.wisecan.unified.service;

import com.wisecan.unified.domain.Company;
import com.wisecan.unified.domain.CompanyInvitation;
import com.wisecan.unified.domain.CompanyRoleLog;
import com.wisecan.unified.domain.CompanySubAccount;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.dto.CompanyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.CompanyInvitationRepository;
import com.wisecan.unified.repository.CompanyRepository;
import com.wisecan.unified.repository.CompanyRoleLogRepository;
import com.wisecan.unified.repository.CompanySubAccountRepository;
import com.wisecan.unified.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock CompanySubAccountRepository subAccountRepository;
    @Mock CompanyInvitationRepository invitationRepository;
    @Mock CompanyRoleLogRepository roleLogRepository;
    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    CompanyService companyService;

    private Company company;
    private Member master;

    @BeforeEach
    void setUp() {
        company = Company.builder()
                .name("테스트 회사")
                .bizNumber("123-45-67890")
                .billingMode("PREPAID")
                .status("ACTIVE")
                .approvedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(company, "id", 1L);

        master = Member.builder()
                .email("master@test.com")
                .password("hashed")
                .name("마스터")
                .phone("01012345678")
                .role(MemberRole.COMPANY_MASTER)
                .status(MemberStatus.ACTIVE)
                .companyId(1L)
                .build();
        ReflectionTestUtils.setField(master, "id", 10L);
        ReflectionTestUtils.setField(master, "createdAt", LocalDateTime.now().minusDays(10));
    }

    // ── 하위 계정 생성 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("하위 계정 생성 성공")
    void createSubAccount_success() {
        given(companyRepository.existsById(1L)).willReturn(true);
        given(subAccountRepository.existsByLoginId("user@test.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");

        CompanySubAccount saved = CompanySubAccount.builder()
                .companyId(1L)
                .loginId("user@test.com")
                .passwordHash("encoded")
                .name("직원")
                .phone("01099998888")
                .build();
        ReflectionTestUtils.setField(saved, "id", 100L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
        given(subAccountRepository.save(any())).willReturn(saved);

        CompanyDto.SubAccountResponse response = companyService.createSubAccount(
                1L, new CompanyDto.CreateSubAccountRequest("user@test.com", "pass1234", "직원", "01099998888"));

        assertThat(response.loginId()).isEqualTo("user@test.com");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("하위 계정 생성 - 중복 로그인 아이디 예외")
    void createSubAccount_duplicateLoginId_throws() {
        given(companyRepository.existsById(1L)).willReturn(true);
        given(subAccountRepository.existsByLoginId("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> companyService.createSubAccount(
                1L, new CompanyDto.CreateSubAccountRequest("dup@test.com", "pass1234", "직원", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 로그인 아이디");
    }

    @Test
    @DisplayName("하위 계정 생성 - 존재하지 않는 회사 예외")
    void createSubAccount_companyNotFound_throws() {
        given(companyRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> companyService.createSubAccount(
                99L, new CompanyDto.CreateSubAccountRequest("user@test.com", "pass1234", "직원", null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── 하위 계정 삭제 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("하위 계정 soft delete 성공")
    void deleteSubAccount_success() {
        CompanySubAccount account = CompanySubAccount.builder()
                .companyId(1L).loginId("sub@test.com").passwordHash("h").name("직원").phone(null).build();
        ReflectionTestUtils.setField(account, "id", 100L);

        given(subAccountRepository.findById(100L)).willReturn(Optional.of(account));

        companyService.deleteSubAccount(1L, 100L);

        assertThat(account.getStatus()).isEqualTo("DELETED");
        assertThat(account.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("하위 계정 삭제 - 다른 회사 계정 예외")
    void deleteSubAccount_wrongCompany_throws() {
        CompanySubAccount account = CompanySubAccount.builder()
                .companyId(99L).loginId("sub@test.com").passwordHash("h").name("직원").phone(null).build();
        ReflectionTestUtils.setField(account, "id", 100L);

        given(subAccountRepository.findById(100L)).willReturn(Optional.of(account));

        assertThatThrownBy(() -> companyService.deleteSubAccount(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 회사의 하위 계정이 아닙니다");
    }

    // ── 마스터 권한 이관 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("마스터 권한 명시적 이관 성공")
    void transferMaster_success() {
        Member newMaster = Member.builder()
                .email("new@test.com").password("h").name("신규마스터").phone(null)
                .role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).companyId(1L).build();
        ReflectionTestUtils.setField(newMaster, "id", 20L);

        given(companyRepository.existsById(1L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(master));
        given(memberRepository.findById(20L)).willReturn(Optional.of(newMaster));
        given(roleLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        companyService.transferMaster(1L, 10L, new CompanyDto.TransferMasterRequest(20L));

        assertThat(master.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(newMaster.getRole()).isEqualTo(MemberRole.COMPANY_MASTER);
        verify(roleLogRepository).save(any(CompanyRoleLog.class));
    }

    @Test
    @DisplayName("마스터 이관 - 본인에게 이관 시 예외")
    void transferMaster_toSelf_throws() {
        given(companyRepository.existsById(1L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(master));

        assertThatThrownBy(() -> companyService.transferMaster(
                1L, 10L, new CompanyDto.TransferMasterRequest(10L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인에게 이관할 수 없습니다");
    }

    @Test
    @DisplayName("마스터 이관 - 다른 회사 회원 지정 시 예외")
    void transferMaster_differentCompany_throws() {
        Member outsider = Member.builder()
                .email("out@test.com").password("h").name("외부").phone(null)
                .role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).companyId(999L).build();
        ReflectionTestUtils.setField(outsider, "id", 20L);

        given(companyRepository.existsById(1L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(master));
        given(memberRepository.findById(20L)).willReturn(Optional.of(outsider));

        assertThatThrownBy(() -> companyService.transferMaster(
                1L, 10L, new CompanyDto.TransferMasterRequest(20L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("같은 회사 소속");
    }

    // ── 마스터 회수 + 자동 전이 ─────────────────────────────────────────────────

    @Test
    @DisplayName("마스터 회수 - 후보 없으면 회사 SUSPENDED")
    void revokeMaster_noCandidate_companySuspended() {
        given(companyRepository.existsById(1L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(master));
        given(memberRepository.findAll()).willReturn(List.of(master));
        given(companyRepository.findById(1L)).willReturn(Optional.of(company));
        given(roleLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        companyService.revokeMaster(1L, 10L, 10L);

        assertThat(master.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(company.getStatus()).isEqualTo("SUSPENDED");
        verify(roleLogRepository).save(any(CompanyRoleLog.class));
    }

    @Test
    @DisplayName("마스터 회수 - 후보 있으면 AUTO_TRANSFER")
    void revokeMaster_withCandidate_autoTransfer() {
        Member candidate = Member.builder()
                .email("cand@test.com").password("h").name("후보").phone(null)
                .role(MemberRole.MEMBER).status(MemberStatus.ACTIVE).companyId(1L).build();
        ReflectionTestUtils.setField(candidate, "id", 30L);
        ReflectionTestUtils.setField(candidate, "createdAt", LocalDateTime.now().minusDays(5));

        given(companyRepository.existsById(1L)).willReturn(true);
        given(memberRepository.findById(10L)).willReturn(Optional.of(master));
        given(memberRepository.findAll()).willReturn(List.of(master, candidate));
        given(roleLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        companyService.revokeMaster(1L, 10L, 10L);

        assertThat(master.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(candidate.getRole()).isEqualTo(MemberRole.COMPANY_MASTER);
        verify(companyRepository, never()).findById(anyLong());
    }

    // ── 권한 이력 조회 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("권한 이력 조회 성공")
    void getRoleLogs_success() {
        given(companyRepository.existsById(1L)).willReturn(true);
        CompanyRoleLog log = CompanyRoleLog.builder()
                .companyId(1L).fromMemberId(null).toMemberId(10L)
                .action("GRANT").reason("INITIAL_BUSINESS_OWNER").actorMemberId(0L).build();
        ReflectionTestUtils.setField(log, "id", 1L);
        ReflectionTestUtils.setField(log, "actedAt", LocalDateTime.now());
        given(roleLogRepository.findByCompanyIdOrderByActedAtDesc(1L)).willReturn(List.of(log));

        List<CompanyDto.RoleLogResponse> result = companyService.getRoleLogs(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).action()).isEqualTo("GRANT");
    }

    // ── 초대 수락 ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("초대 수락 성공")
    void acceptInvitation_success() {
        // SHA-256 hex of "valid-token"
        String rawToken = "valid-token";

        CompanyInvitation invitation = CompanyInvitation.builder()
                .companyId(1L).inviterMemberId(10L)
                .inviteeEmail("new@test.com").inviteePhone(null)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(invitation, "id", 5L);
        ReflectionTestUtils.setField(invitation, "invitedAt", LocalDateTime.now());

        given(invitationRepository.findByTokenHash(sha256Hex(rawToken))).willReturn(Optional.of(invitation));
        given(subAccountRepository.existsByLoginId("new@test.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");

        CompanySubAccount saved = CompanySubAccount.builder()
                .companyId(1L).loginId("new@test.com").passwordHash("encoded").name("신규").phone(null).build();
        ReflectionTestUtils.setField(saved, "id", 200L);
        ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());
        given(subAccountRepository.save(any())).willReturn(saved);

        CompanyDto.SubAccountResponse response = companyService.acceptInvitation(
                new CompanyDto.AcceptInvitationRequest(rawToken, "pass1234", "신규", null));

        assertThat(response.loginId()).isEqualTo("new@test.com");
        assertThat(invitation.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("초대 수락 - 만료된 토큰 예외")
    void acceptInvitation_expired_throws() {
        String rawToken = "expired-token";

        CompanyInvitation invitation = CompanyInvitation.builder()
                .companyId(1L).inviterMemberId(10L)
                .inviteeEmail("new@test.com").inviteePhone(null)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(LocalDateTime.now().minusDays(1))   // 이미 만료
                .build();
        ReflectionTestUtils.setField(invitation, "invitedAt", LocalDateTime.now().minusDays(8));

        given(invitationRepository.findByTokenHash(sha256Hex(rawToken))).willReturn(Optional.of(invitation));

        assertThatThrownBy(() -> companyService.acceptInvitation(
                new CompanyDto.AcceptInvitationRequest(rawToken, "pass1234", "신규", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
