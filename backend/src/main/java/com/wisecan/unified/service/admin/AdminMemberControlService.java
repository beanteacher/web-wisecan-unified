package com.wisecan.unified.service.admin;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.admin.ControlAction;
import com.wisecan.unified.domain.admin.MemberControlAuditLog;
import com.wisecan.unified.dto.admin.AdminMemberControlDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.admin.MemberControlAuditLogRepository;
import com.wisecan.unified.service.sendernumber.CallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 운영자 회원 통제 서비스 — §12.3.
 *
 * 정책 위반 회원 SUSPENDED 또는 TERMINATED 전이 시
 * 보유 발신번호 일괄 해지(DELETED) + API Key 일괄 폐기 + 사유 감사 로그 를 단일 트랜잭션으로 처리한다.
 *
 * RQ-ADMIN-203·207·208, RQ-CALLBACK-103 후속
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMemberControlService {

    private final MemberRepository memberRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final CallbackService callbackService;
    private final MemberControlAuditLogRepository auditLogRepository;

    // ── §12.3 강제 정지 ──────────────────────────────────────────────────

    /**
     * 회원 강제 정지 — ACTIVE → SUSPENDED.
     *
     * 단일 트랜잭션:
     *   1) Member.suspend()
     *   2) 발신번호 일괄 해지 (CallbackService.deleteAllByMember)
     *   3) API Key 일괄 폐기 (REVOKED)
     *   4) 감사 로그 INSERT
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberControlDto.MemberStatusResponse suspend(Long memberId, Long operatorId, String reason) {
        Member member = findMember(memberId);
        assertNotAdmin(member);

        member.suspend();

        revokeAllApiKeys(memberId, operatorId);
        callbackService.deleteAllByMember(memberId, operatorId);

        auditLogRepository.save(MemberControlAuditLog.builder()
                .memberId(memberId)
                .action(ControlAction.SUSPEND)
                .reason(reason)
                .operatorId(operatorId)
                .build());

        log.info("[회원 정지] memberId={} operatorId={} reason={}", memberId, operatorId, reason);
        return AdminMemberControlDto.MemberStatusResponse.from(member);
    }

    // ── §12.3 강제 해지 ──────────────────────────────────────────────────

    /**
     * 회원 강제 해지 — ACTIVE/SUSPENDED → TERMINATED.
     *
     * 단일 트랜잭션:
     *   1) Member.terminate()
     *   2) 발신번호 일괄 해지
     *   3) API Key 일괄 폐기
     *   4) 감사 로그 INSERT
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberControlDto.MemberStatusResponse terminate(Long memberId, Long operatorId, String reason) {
        Member member = findMember(memberId);
        assertNotAdmin(member);

        member.terminate();

        revokeAllApiKeys(memberId, operatorId);
        callbackService.deleteAllByMember(memberId, operatorId);

        auditLogRepository.save(MemberControlAuditLog.builder()
                .memberId(memberId)
                .action(ControlAction.TERMINATE)
                .reason(reason)
                .operatorId(operatorId)
                .build());

        log.info("[회원 해지] memberId={} operatorId={} reason={}", memberId, operatorId, reason);
        return AdminMemberControlDto.MemberStatusResponse.from(member);
    }

    // ── §12.3 정지 해제 ──────────────────────────────────────────────────

    /**
     * 회원 정지 해제 — SUSPENDED → ACTIVE.
     * 발신번호·API Key는 복원하지 않는다 (재등록은 회원이 직접 수행).
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminMemberControlDto.MemberStatusResponse unsuspend(Long memberId, Long operatorId, String reason) {
        Member member = findMember(memberId);

        member.unsuspend();

        auditLogRepository.save(MemberControlAuditLog.builder()
                .memberId(memberId)
                .action(ControlAction.UNSUSPEND)
                .reason(reason)
                .operatorId(operatorId)
                .build());

        log.info("[회원 정지 해제] memberId={} operatorId={} reason={}", memberId, operatorId, reason);
        return AdminMemberControlDto.MemberStatusResponse.from(member);
    }

    // ── 감사 로그 조회 ────────────────────────────────────────────────────

    /**
     * 특정 회원의 통제 이력 조회.
     *
     * M-3 리뷰 반영: memberId 는 파라미터로 고정이므로 루프 바깥에서 한 번만 조회한다.
     * 기존 구현은 스트림 내부에서 매 항목마다 memberRepository.findById() 를 호출해
     * 로그 N 건 → DB N 번 쿼리가 발생했다.
     */
    @Transactional(readOnly = true)
    public List<AdminMemberControlDto.ControlAuditEntry> listAuditLog(Long memberId) {
        String memberEmail = memberRepository.findById(memberId)
                .map(Member::getEmail)
                .orElse("-");

        return auditLogRepository.findByMemberIdOrderByOccurredAtDesc(memberId)
                .stream()
                .map(log -> new AdminMemberControlDto.ControlAuditEntry(
                        log.getMemberId(),
                        memberEmail,
                        log.getAction(),
                        log.getReason(),
                        log.getOperatorId(),
                        log.getOccurredAt()))
                .toList();
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member", memberId));
    }

    /**
     * 운영자 계정 자신에게 통제 조작을 가하는 것을 방지한다.
     * ADMIN/SUPER_ADMIN 역할 회원은 이 API로 처리하지 않는다.
     */
    private void assertNotAdmin(Member member) {
        if (member.getRole() == MemberRole.ADMIN || member.getRole() == MemberRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("운영자 계정은 이 API로 통제할 수 없습니다.");
        }
    }

    /** 회원의 모든 활성 API Key를 일괄 폐기 */
    private void revokeAllApiKeys(Long memberId, Long operatorId) {
        List<ApiKey> activeKeys = apiKeyRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId)
                .stream()
                .filter(k -> k.getStatus() == ApiKeyStatus.ACTIVE
                        || k.getStatus() == ApiKeyStatus.PENDING_REVIEW)
                .toList();

        for (ApiKey key : activeKeys) {
            key.revoke();
        }

        log.info("[API Key 일괄 폐기] memberId={} operatorId={} count={}", memberId, operatorId, activeKeys.size());
    }
}
