package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.template.TemplateTransferQueue;
import com.wisecan.unified.domain.template.TemplateTransferStatus;
import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.template.TemplateTransferQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SMS17 → WiseCan 이관 처리 큐 서비스.
 *
 * 이관 신청 플로우: PENDING → IN_PROGRESS → COMPLETED / REJECTED / CANCELLED
 * - 회원은 이관 신청 및 본인 신청 취소만 가능.
 * - 운영자는 IN_PROGRESS 전환, COMPLETED / REJECTED 처리 가능.
 * 02_FEATURE_SPEC §9.1 이관 처리 참조.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateTransferService {

    private static final List<TemplateTransferStatus> ACTIVE_STATUSES =
            List.of(TemplateTransferStatus.PENDING, TemplateTransferStatus.IN_PROGRESS);

    private final TemplateTransferQueueRepository transferQueueRepository;
    private final MemberRepository memberRepository;

    // ── 회원 — 이관 신청 ─────────────────────────────────────────

    /**
     * SMS17 이관 신청 등록.
     * 동일 소스 템플릿 코드로 진행 중인 신청이 있으면 예외.
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDto.TransferResponse requestTransfer(String email, TemplateDto.TransferRequest request) {
        Member member = findMemberByEmail(email);

        // 중복 신청 방지
        if (transferQueueRepository.existsByMemberIdAndSourceTemplateCodeAndStatusIn(
                member.getId(), request.sourceTemplateCode(), ACTIVE_STATUSES)) {
            throw new IllegalStateException(
                    "이미 진행 중인 이관 신청이 존재합니다: " + request.sourceTemplateCode());
        }

        TemplateTransferQueue queue = TemplateTransferQueue.builder()
                .memberId(member.getId())
                .sourceTemplateCode(request.sourceTemplateCode())
                .kkoProfileNo(request.kkoProfileNo())
                .reason(request.reason())
                .build();

        TemplateTransferQueue saved = transferQueueRepository.save(queue);
        log.info("이관 신청 등록: memberId={}, sourceTemplateCode={}, id={}",
                member.getId(), request.sourceTemplateCode(), saved.getId());

        return TemplateDto.TransferResponse.from(saved);
    }

    /**
     * 회원 — 본인 이관 신청 취소 (PENDING 상태에서만 가능).
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTransfer(String email, Long transferId) {
        Member member = findMemberByEmail(email);
        TemplateTransferQueue queue = findQueueById(transferId);

        if (!member.getId().equals(queue.getMemberId())) {
            throw new IllegalArgumentException("본인의 이관 신청만 취소할 수 있습니다.");
        }
        if (queue.getStatus() != TemplateTransferStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 취소 가능합니다. 현재 상태: " + queue.getStatus());
        }

        queue.cancel();
        log.info("이관 신청 취소: memberId={}, transferId={}", member.getId(), transferId);
    }

    /**
     * 회원 — 본인 이관 신청 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<TemplateDto.TransferResponse> listMyTransfers(String email) {
        Member member = findMemberByEmail(email);
        return transferQueueRepository.findByMemberIdOrderByRequestedAtDesc(member.getId())
                .stream()
                .map(TemplateDto.TransferResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 회원 — 이관 신청 단건 조회.
     */
    @Transactional(readOnly = true)
    public TemplateDto.TransferDetail detailMyTransfer(String email, Long transferId) {
        Member member = findMemberByEmail(email);
        TemplateTransferQueue queue = findQueueById(transferId);

        if (!member.getId().equals(queue.getMemberId())) {
            throw new IllegalArgumentException("본인의 이관 신청만 조회할 수 있습니다.");
        }
        return TemplateDto.TransferDetail.from(queue);
    }

    // ── 운영자 — 이관 처리 큐 ────────────────────────────────────

    /**
     * 운영자 — PENDING 이관 신청 목록 조회 (처리 큐).
     */
    @Transactional(readOnly = true)
    public List<TemplateDto.TransferDetail> listPendingQueue() {
        return transferQueueRepository.findByStatusOrderByRequestedAtAsc(TemplateTransferStatus.PENDING)
                .stream()
                .map(TemplateDto.TransferDetail::from)
                .collect(Collectors.toList());
    }

    /**
     * 운영자 — 이관 처리 시작 (PENDING → IN_PROGRESS).
     */
    @Transactional(rollbackFor = Exception.class)
    public void startProcess(Long transferId, Long operatorId) {
        TemplateTransferQueue queue = findQueueById(transferId);
        if (queue.getStatus() != TemplateTransferStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 처리 시작 가능합니다. 현재 상태: " + queue.getStatus());
        }
        queue.startProcess(operatorId);
        log.info("이관 처리 시작: transferId={}, operatorId={}", transferId, operatorId);
    }

    /**
     * 운영자 — 이관 완료 처리 또는 거부 (IN_PROGRESS → COMPLETED / REJECTED).
     */
    @Transactional(rollbackFor = Exception.class)
    public TemplateDto.TransferDetail processTransfer(Long transferId, Long operatorId,
                                                       TemplateDto.TransferProcessRequest request) {
        TemplateTransferQueue queue = findQueueById(transferId);
        if (queue.getStatus() != TemplateTransferStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "IN_PROGRESS 상태에서만 완료/거부 처리 가능합니다. 현재 상태: " + queue.getStatus());
        }

        if (request.approve()) {
            queue.complete(operatorId);
            log.info("이관 완료 처리: transferId={}, operatorId={}", transferId, operatorId);
        } else {
            if (request.rejectReason() == null || request.rejectReason().isBlank()) {
                throw new IllegalArgumentException("거부 시 사유를 입력해야 합니다.");
            }
            queue.reject(operatorId, request.rejectReason());
            log.info("이관 거부 처리: transferId={}, operatorId={}, reason={}",
                    transferId, operatorId, request.rejectReason());
        }

        return TemplateDto.TransferDetail.from(queue);
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + email));
    }

    private TemplateTransferQueue findQueueById(Long transferId) {
        return transferQueueRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "이관 신청을 찾을 수 없습니다: " + transferId));
    }
}
