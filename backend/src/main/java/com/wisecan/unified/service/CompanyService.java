package com.wisecan.unified.service;

import com.wisecan.unified.domain.Company;
import com.wisecan.unified.domain.CompanyInvitation;
import com.wisecan.unified.domain.CompanyRoleLog;
import com.wisecan.unified.domain.CompanySubAccount;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.dto.CompanyDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.CompanyInvitationRepository;
import com.wisecan.unified.repository.CompanyRepository;
import com.wisecan.unified.repository.CompanyRoleLogRepository;
import com.wisecan.unified.repository.CompanySubAccountRepository;
import com.wisecan.unified.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanySubAccountRepository subAccountRepository;
    private final CompanyInvitationRepository invitationRepository;
    private final CompanyRoleLogRepository roleLogRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // ── 회사 정보 조회 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CompanyDto.CompanyInfoResponse getCompanyInfo(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company", companyId));
        return toCompanyInfoResponse(company);
    }

    // ── 하위 계정 목록 조회 ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CompanyDto.SubAccountListResponse listSubAccounts(Long companyId) {
        assertCompanyExists(companyId);
        List<CompanySubAccount> accounts = subAccountRepository
                .findByCompanyIdAndStatusNot(companyId, "DELETED");
        List<CompanyDto.SubAccountResponse> responses = accounts.stream()
                .map(this::toSubAccountResponse)
                .toList();
        return new CompanyDto.SubAccountListResponse(responses, responses.size());
    }

    // ── 하위 계정 직접 생성 ─────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public CompanyDto.SubAccountResponse createSubAccount(Long companyId,
                                                           CompanyDto.CreateSubAccountRequest request) {
        assertCompanyExists(companyId);
        if (subAccountRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
        }
        CompanySubAccount account = CompanySubAccount.builder()
                .companyId(companyId)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .build();
        CompanySubAccount saved = subAccountRepository.save(account);
        log.info("하위 계정 생성 companyId={} loginId={}", companyId, saved.getLoginId());
        return toSubAccountResponse(saved);
    }

    // ── 하위 계정 비활성화 ──────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public CompanyDto.SubAccountResponse disableSubAccount(Long companyId, Long subAccountId) {
        CompanySubAccount account = getSubAccountOfCompany(companyId, subAccountId);
        account.disable();
        log.info("하위 계정 비활성화 companyId={} subAccountId={}", companyId, subAccountId);
        return toSubAccountResponse(account);
    }

    // ── 하위 계정 삭제(soft delete) ─────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void deleteSubAccount(Long companyId, Long subAccountId) {
        CompanySubAccount account = getSubAccountOfCompany(companyId, subAccountId);
        account.softDelete();
        log.info("하위 계정 삭제 companyId={} subAccountId={}", companyId, subAccountId);
    }

    // ── 초대 링크 발행 ──────────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public CompanyDto.InvitationResponse createInvitation(Long companyId, Long inviterMemberId,
                                                           CompanyDto.CreateInvitationRequest request) {
        assertCompanyExists(companyId);
        if (invitationRepository.existsByInviteeEmailAndCompanyIdAndStatus(
                request.inviteeEmail(), companyId, "PENDING")) {
            throw new IllegalArgumentException("이미 초대 대기 중인 이메일입니다.");
        }
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256Hex(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        CompanyInvitation invitation = CompanyInvitation.builder()
                .companyId(companyId)
                .inviterMemberId(inviterMemberId)
                .inviteeEmail(request.inviteeEmail())
                .inviteePhone(request.inviteePhone())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        CompanyInvitation saved = invitationRepository.save(invitation);
        log.info("초대 링크 발행 companyId={} inviteeEmail={}", companyId, request.inviteeEmail());
        return new CompanyDto.InvitationResponse(
                saved.getId(), saved.getInviteeEmail(), saved.getStatus(), rawToken, expiresAt);
    }

    // ── 초대 수락 → 하위 계정 생성 ─────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public CompanyDto.SubAccountResponse acceptInvitation(CompanyDto.AcceptInvitationRequest request) {
        String tokenHash = sha256Hex(request.token());
        CompanyInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 초대 토큰입니다."));

        if (!invitation.isUsable()) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 초대 링크입니다.");
        }
        if (subAccountRepository.existsByLoginId(invitation.getInviteeEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
        }

        CompanySubAccount account = CompanySubAccount.builder()
                .companyId(invitation.getCompanyId())
                .loginId(invitation.getInviteeEmail())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .phone(request.phone())
                .build();
        CompanySubAccount saved = subAccountRepository.save(account);
        invitation.accept();
        log.info("초대 수락 companyId={} loginId={}", invitation.getCompanyId(), saved.getLoginId());
        return toSubAccountResponse(saved);
    }

    // ── 마스터 권한 명시적 이관 (TRANSFER) ─────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public void transferMaster(Long companyId, Long currentMasterId,
                                CompanyDto.TransferMasterRequest request) {
        assertCompanyExists(companyId);
        Member currentMaster = getMasterOfCompany(companyId, currentMasterId);
        Member newMaster = memberRepository.findById(request.toMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Member", request.toMemberId()));

        if (!companyId.equals(newMaster.getCompanyId())) {
            throw new IllegalArgumentException("같은 회사 소속 회원에게만 권한을 이관할 수 있습니다.");
        }
        if (newMaster.getId().equals(currentMasterId)) {
            throw new IllegalArgumentException("본인에게 이관할 수 없습니다.");
        }

        // 단일 트랜잭션: 강등 → 승격 → 로그 (순서 중요 — 부분 UNIQUE 제약 고려)
        currentMaster.demoteToMember();
        newMaster.promoteToCompanyMaster(companyId);

        roleLogRepository.save(CompanyRoleLog.builder()
                .companyId(companyId)
                .fromMemberId(currentMasterId)
                .toMemberId(newMaster.getId())
                .action("TRANSFER")
                .reason("USER_TRANSFER")
                .actorMemberId(currentMasterId)
                .build());

        log.info("마스터 권한 이관 companyId={} from={} to={}", companyId, currentMasterId, newMaster.getId());
    }

    // ── 마스터 권한 회수 + 자동 전이 (AUTO_TRANSFER / REVOKE) ──────────────────

    @Transactional(rollbackFor = Exception.class)
    public void revokeMaster(Long companyId, Long targetMemberId, Long actorMemberId) {
        assertCompanyExists(companyId);
        Member targetMaster = getMasterOfCompany(companyId, targetMemberId);

        // 후보 선출: 가장 먼저 가입한 활성 MEMBER
        List<Member> candidates = memberRepository.findAll().stream()
                .filter(m -> companyId.equals(m.getCompanyId())
                        && !m.getId().equals(targetMemberId)
                        && m.getRole() == MemberRole.MEMBER
                        && "ACTIVE".equals(m.getStatus().name()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    int c = a.getCreatedAt().compareTo(b.getCreatedAt());
                    return c != 0 ? c : a.getId().compareTo(b.getId());
                })
                .toList();

        targetMaster.demoteToMember();

        if (candidates.isEmpty()) {
            // 후보 없음 → 회사 SUSPENDED
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new EntityNotFoundException("Company", companyId));
            company.suspend();

            roleLogRepository.save(CompanyRoleLog.builder()
                    .companyId(companyId)
                    .fromMemberId(targetMemberId)
                    .toMemberId(null)
                    .action("REVOKE")
                    .reason("LAST_MEMBER_LEFT")
                    .actorMemberId(actorMemberId)
                    .build());
            log.info("마스터 회수 후 후보 없음 → 회사 SUSPENDED companyId={}", companyId);
        } else {
            Member nextMaster = candidates.get(0);
            nextMaster.promoteToCompanyMaster(companyId);

            roleLogRepository.save(CompanyRoleLog.builder()
                    .companyId(companyId)
                    .fromMemberId(targetMemberId)
                    .toMemberId(nextMaster.getId())
                    .action("AUTO_TRANSFER")
                    .reason("MASTER_REVOKED_AUTO")
                    .actorMemberId(actorMemberId)
                    .build());
            log.info("마스터 자동 전이 companyId={} to={}", companyId, nextMaster.getId());
        }
    }

    // ── 권한 이력 조회 ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CompanyDto.RoleLogResponse> getRoleLogs(Long companyId) {
        assertCompanyExists(companyId);
        return roleLogRepository.findByCompanyIdOrderByActedAtDesc(companyId).stream()
                .map(log -> new CompanyDto.RoleLogResponse(
                        log.getId(),
                        log.getAction(),
                        log.getReason(),
                        log.getFromMemberId(),
                        log.getToMemberId(),
                        log.getActorMemberId(),
                        log.getActedAt()))
                .toList();
    }

    // ── 내부 헬퍼 ───────────────────────────────────────────────────────────────

    private void assertCompanyExists(Long companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new EntityNotFoundException("Company", companyId);
        }
    }

    private Member getMasterOfCompany(Long companyId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId));
        if (!companyId.equals(member.getCompanyId())
                || member.getRole() != MemberRole.COMPANY_MASTER) {
            throw new IllegalArgumentException("해당 회사의 마스터 권한이 없습니다.");
        }
        return member;
    }

    private CompanySubAccount getSubAccountOfCompany(Long companyId, Long subAccountId) {
        CompanySubAccount account = subAccountRepository.findById(subAccountId)
                .orElseThrow(() -> new EntityNotFoundException("CompanySubAccount", subAccountId));
        if (!companyId.equals(account.getCompanyId())) {
            throw new IllegalArgumentException("해당 회사의 하위 계정이 아닙니다.");
        }
        if ("DELETED".equals(account.getStatus())) {
            throw new IllegalArgumentException("이미 삭제된 계정입니다.");
        }
        return account;
    }

    private CompanyDto.SubAccountResponse toSubAccountResponse(CompanySubAccount a) {
        return new CompanyDto.SubAccountResponse(
                a.getId(), a.getCompanyId(), a.getLoginId(),
                a.getName(), a.getPhone(), a.getStatus(), a.getCreatedAt());
    }

    private CompanyDto.CompanyInfoResponse toCompanyInfoResponse(Company c) {
        return new CompanyDto.CompanyInfoResponse(
                c.getId(), c.getName(), c.getBizNumber(),
                c.getBillingMode(), c.getStatus(), c.getApprovedAt());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
