package com.wisecan.unified.service.sendernumber;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackLog;
import com.wisecan.unified.domain.sendernumber.CallbackLogEvent;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.sendernumber.CallbackDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.sendernumber.CallbackDocumentRepository;
import com.wisecan.unified.repository.sendernumber.CallbackLogRepository;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 발신번호 등록 서비스 — 4 케이스(SELF_MOBILE / SELF_LANDLINE / EMPLOYEE / CORP_REP).
 *
 * - SELF_MOBILE  : 본인 인증 휴대폰 번호와 일치 시 즉시 REGISTERED
 * - SELF_LANDLINE: 비-휴대폰 번호 즉시 REGISTERED (통신사 명의 확인은 외부 연동)
 * - EMPLOYEE     : SUBMITTED 저장 → 운영자 심사 큐
 * - CORP_REP     : SUBMITTED 저장 → 운영자 심사 큐
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackService {

    private final CallbackRepository callbackRepository;
    private final CallbackDocumentRepository callbackDocumentRepository;
    private final CallbackLogRepository callbackLogRepository;
    private final MemberRepository memberRepository;

    /** 활성 상태 목록 (활성 1개 강제 검증에 사용) */
    private static final List<CallbackStatus> ACTIVE_STATUSES =
        List.of(CallbackStatus.SUBMITTED, CallbackStatus.UNDER_REVIEW, CallbackStatus.REGISTERED);

    // ── 등록 ─────────────────────────────────────────────────────

    /**
     * 발신번호 등록 (4 케이스 공통 진입점).
     * registerType 에 따라 즉시 REGISTERED 또는 SUBMITTED 로 분기.
     */
    @Transactional(rollbackFor = Exception.class)
    public CallbackDto.RegisterResponse register(String email, CallbackDto.RegisterRequest request) {
        Member member = findMemberByEmail(email);
        String normalizedPhone = normalizePhone(request.phoneNumber());

        // 활성 등록 1개 강제 검증
        if (callbackRepository.existsByPhoneNumberAndStatusIn(normalizedPhone, ACTIVE_STATUSES)) {
            throw new IllegalStateException("이미 등록되었거나 심사 중인 발신번호입니다: " + normalizedPhone);
        }

        CallbackRegisterType type = request.registerType();
        Callback callback;

        if (type == CallbackRegisterType.SELF_MOBILE) {
            callback = registerSelfMobile(member, normalizedPhone, request.description());
        } else if (type == CallbackRegisterType.SELF_LANDLINE) {
            callback = registerSelfLandline(member, normalizedPhone, request.description());
        } else {
            // EMPLOYEE / CORP_REP — 서류 심사형
            callback = registerReviewType(member, normalizedPhone, type, request.description());
        }

        Callback saved = callbackRepository.save(callback);
        appendLog(saved, saved.isRegistered() ? CallbackLogEvent.REGISTERED : null, member.getId(), null, null);

        String message = saved.isRegistered()
            ? "발신번호가 즉시 등록되었습니다."
            : "등록 신청이 접수되었습니다. 운영자 심사 후 승인됩니다.";

        log.info("발신번호 등록: memberId={}, phone={}, type={}, status={}",
            member.getId(), normalizedPhone, type, saved.getStatus());

        return new CallbackDto.RegisterResponse(saved.getId(), saved.getPhoneNumber(), saved.getStatus(), message);
    }

    /** §4.1 SELF_MOBILE — 본인 인증 휴대폰 번호와 일치 시 즉시 REGISTERED */
    private Callback registerSelfMobile(Member member, String phoneNumber, String description) {
        String memberPhone = normalizePhone(member.getPhone());
        if (!phoneNumber.equals(memberPhone)) {
            throw new IllegalArgumentException(
                "본인 인증 휴대폰 번호와 일치하지 않습니다. SELF_MOBILE 케이스는 본인 인증 번호만 등록 가능합니다.");
        }
        Callback cb = Callback.builder()
            .memberId(member.getId())
            .phoneNumber(phoneNumber)
            .registerType(CallbackRegisterType.SELF_MOBILE)
            .description(description)
            .status(CallbackStatus.REGISTERED)
            .build();
        cb.registerImmediately();
        return cb;
    }

    /** §4.2 SELF_LANDLINE — 비-휴대폰 번호 즉시 REGISTERED (통신사 명의 확인은 외부 연동) */
    private Callback registerSelfLandline(Member member, String phoneNumber, String description) {
        Callback cb = Callback.builder()
            .memberId(member.getId())
            .phoneNumber(phoneNumber)
            .registerType(CallbackRegisterType.SELF_LANDLINE)
            .description(description)
            .status(CallbackStatus.REGISTERED)
            .build();
        cb.registerImmediately();
        return cb;
    }

    /** §4.3 EMPLOYEE / CORP_REP — 심사 큐 등록 (SUBMITTED) */
    private Callback registerReviewType(Member member, String phoneNumber,
                                         CallbackRegisterType type, String description) {
        Callback cb = Callback.builder()
            .memberId(member.getId())
            .companyId(member.getCompanyId())
            .phoneNumber(phoneNumber)
            .registerType(type)
            .description(description)
            .status(CallbackStatus.SUBMITTED)
            .build();
        return cb;
    }

    // ── 삭제 ─────────────────────────────────────────────────────

    /** §4.4 발신번호 삭제 (회원 직접 삭제) */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String email, Long callbackId) {
        Member member = findMemberByEmail(email);
        Callback callback = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("발신번호를 찾을 수 없습니다: " + callbackId));

        // 본인 소유 검증
        if (!member.getId().equals(callback.getMemberId())) {
            throw new IllegalArgumentException("본인의 발신번호만 삭제할 수 있습니다.");
        }

        callback.delete();
        appendLog(callback, CallbackLogEvent.DELETED, member.getId(), null, null);

        log.info("발신번호 삭제: memberId={}, callbackId={}, phone={}",
            member.getId(), callbackId, callback.getPhoneNumber());
    }

    /**
     * 회원 탈퇴·정지 연쇄 삭제 — 회원의 모든 활성 발신번호를 DELETED 처리.
     * 운영자 또는 시스템 호출 (actorOperatorId 사용).
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByMember(Long memberId, Long actorOperatorId) {
        List<Callback> actives = callbackRepository.findByMemberIdAndStatusNot(memberId, CallbackStatus.DELETED);
        for (Callback cb : actives) {
            if (cb.isActive()) {
                cb.delete();
                appendLog(cb, CallbackLogEvent.DELETED, null, actorOperatorId, "회원 연쇄 삭제");
            }
        }
        log.info("발신번호 연쇄 삭제: memberId={}, count={}", memberId, actives.size());
    }

    // ── 조회 ─────────────────────────────────────────────────────

    /** 회원의 발신번호 목록 (DELETED 제외) */
    @Transactional(readOnly = true)
    public List<CallbackDto.Summary> list(String email) {
        Member member = findMemberByEmail(email);
        return callbackRepository.findByMemberIdAndStatusNot(member.getId(), CallbackStatus.DELETED)
            .stream()
            .map(CallbackDto.Summary::from)
            .collect(Collectors.toList());
    }

    /** 발신번호 단건 조회 */
    @Transactional(readOnly = true)
    public CallbackDto.Response detail(String email, Long callbackId) {
        Member member = findMemberByEmail(email);
        Callback callback = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("발신번호를 찾을 수 없습니다: " + callbackId));

        if (!member.getId().equals(callback.getMemberId())) {
            throw new IllegalArgumentException("본인의 발신번호만 조회할 수 있습니다.");
        }
        return CallbackDto.Response.from(callback);
    }

    // ── 운영자 심사 (W-106 운영자 콘솔 검토 큐에서 호출) ─────────────

    /**
     * 운영자 승인 — SUBMITTED / UNDER_REVIEW → REGISTERED.
     * W-106 운영자 콘솔에서 호출한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveCallback(Long callbackId, Long actorOperatorId) {
        Callback callback = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("발신번호를 찾을 수 없습니다: " + callbackId));

        if (callback.getStatus() != CallbackStatus.SUBMITTED
                && callback.getStatus() != CallbackStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 대기 또는 검토 중 상태가 아닙니다: " + callback.getStatus());
        }

        callback.approve();
        appendLog(callback, CallbackLogEvent.REVIEW_APPROVED, null, actorOperatorId, null);

        log.info("발신번호 승인: callbackId={}, operatorId={}, phone={}",
            callbackId, actorOperatorId, callback.getPhoneNumber());
    }

    /**
     * 운영자 반려 — SUBMITTED / UNDER_REVIEW → REJECTED.
     * W-106 운영자 콘솔에서 호출한다.
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectCallback(Long callbackId, Long actorOperatorId, String reason) {
        Callback callback = callbackRepository.findById(callbackId)
            .orElseThrow(() -> new EntityNotFoundException("발신번호를 찾을 수 없습니다: " + callbackId));

        if (callback.getStatus() != CallbackStatus.SUBMITTED
                && callback.getStatus() != CallbackStatus.UNDER_REVIEW) {
            throw new IllegalStateException("심사 대기 또는 검토 중 상태가 아닙니다: " + callback.getStatus());
        }

        callback.reject(reason);
        appendLog(callback, CallbackLogEvent.REVIEW_REJECTED, null, actorOperatorId, reason);

        log.info("발신번호 반려: callbackId={}, operatorId={}, reason={}",
            callbackId, actorOperatorId, reason);
    }

    /**
     * 운영자 심사 큐 조회 — SUBMITTED 상태 전체 목록.
     * W-106 운영자 콘솔 검토 큐 진입점.
     */
    @Transactional(readOnly = true)
    public List<CallbackDto.Summary> listPendingReview() {
        return callbackRepository.findByStatusIn(List.of(CallbackStatus.SUBMITTED, CallbackStatus.UNDER_REVIEW))
            .stream()
            .map(CallbackDto.Summary::from)
            .collect(Collectors.toList());
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + email));
    }

    /**
     * 전화번호 정규화 — 하이픈 제거, 국가코드 +82 처리.
     * 예: "010-1234-5678" → "01012345678", "+821012345678" → "01012345678"
     */
    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("82") && digits.length() > 10) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    private void appendLog(Callback callback, CallbackLogEvent event,
                            Long actorMemberId, Long actorOperatorId, String comment) {
        if (event == null) return;
        CallbackLog log = CallbackLog.builder()
            .callbackId(callback.getId())
            .phoneNumber(callback.getPhoneNumber())
            .event(event)
            .actorMemberId(actorMemberId)
            .actorOperatorId(actorOperatorId)
            .comment(comment)
            .build();
        callbackLogRepository.save(log);
    }
}
