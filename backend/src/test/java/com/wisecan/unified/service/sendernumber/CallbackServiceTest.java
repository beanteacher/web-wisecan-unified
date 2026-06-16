package com.wisecan.unified.service.sendernumber;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.sendernumber.CallbackDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.sendernumber.CallbackDocumentRepository;
import com.wisecan.unified.repository.sendernumber.CallbackLogRepository;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CallbackServiceTest {

    @Mock
    private CallbackRepository callbackRepository;

    @Mock
    private CallbackDocumentRepository callbackDocumentRepository;

    @Mock
    private CallbackLogRepository callbackLogRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CallbackService callbackService;

    // ── 공통 픽스처 ──────────────────────────────────────────────

    private Member memberWithPhone(String email, String phone) {
        return Member.builder()
            .email(email)
            .password("hashed")
            .name("홍길동")
            .phone(phone)
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    private Callback savedCallback(Long id, String phone, CallbackRegisterType type, CallbackStatus status) {
        Callback cb = Callback.builder()
            .memberId(1L)
            .phoneNumber(phone)
            .registerType(type)
            .description("테스트 발신번호")
            .status(status)
            .build();
        // 리플렉션 없이 id 는 save 반환값으로 처리 — Mockito 가 반환
        return cb;
    }

    // ── §4.1 SELF_MOBILE 즉시 등록 ─────────────────────────────

    @Nested
    @DisplayName("§4.1 SELF_MOBILE — 본인 휴대폰 즉시 등록")
    class SelfMobileTest {

        @Test
        @DisplayName("본인 인증 휴대폰과 일치하면 즉시 REGISTERED")
        void register_selfMobile_matchesPhone_immediateRegistered() {
            String email = "user@test.com";
            String phone = "01012345678";

            Member member = memberWithPhone(email, phone);
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.existsByPhoneNumberAndStatusIn(any(), any())).willReturn(false);

            Callback saved = savedCallback(1L, phone, CallbackRegisterType.SELF_MOBILE, CallbackStatus.REGISTERED);
            given(callbackRepository.save(any(Callback.class))).willReturn(saved);

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                phone, CallbackRegisterType.SELF_MOBILE, "고객 문자 발송용"
            );

            CallbackDto.RegisterResponse resp = callbackService.register(email, req);

            assertThat(resp.status()).isEqualTo(CallbackStatus.REGISTERED);
            verify(callbackRepository).save(any(Callback.class));
            verify(callbackLogRepository).save(any());
        }

        @Test
        @DisplayName("본인 인증 휴대폰과 불일치하면 IllegalArgumentException")
        void register_selfMobile_mismatchPhone_throws() {
            String email = "user@test.com";
            Member member = memberWithPhone(email, "01099999999");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                "01012345678", CallbackRegisterType.SELF_MOBILE, "발신용"
            );

            assertThatThrownBy(() -> callbackService.register(email, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 인증 휴대폰");

            verify(callbackRepository, never()).save(any());
        }
    }

    // ── §4.2 SELF_LANDLINE 즉시 등록 ────────────────────────────

    @Nested
    @DisplayName("§4.2 SELF_LANDLINE — 개인 비-휴대폰 등록")
    class SelfLandlineTest {

        @Test
        @DisplayName("SELF_LANDLINE 요청은 REGISTERED 상태로 즉시 등록")
        void register_selfLandline_immediateRegistered() {
            String email = "user@test.com";
            Member member = memberWithPhone(email, "01012345678");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.existsByPhoneNumberAndStatusIn(any(), any())).willReturn(false);

            Callback saved = savedCallback(1L, "0215551234", CallbackRegisterType.SELF_LANDLINE, CallbackStatus.REGISTERED);
            given(callbackRepository.save(any(Callback.class))).willReturn(saved);

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                "0215551234", CallbackRegisterType.SELF_LANDLINE, "사무실 대표번호"
            );

            CallbackDto.RegisterResponse resp = callbackService.register(email, req);

            assertThat(resp.status()).isEqualTo(CallbackStatus.REGISTERED);
            verify(callbackRepository).save(any(Callback.class));
        }
    }

    // ── §4.3 EMPLOYEE / CORP_REP 심사형 ─────────────────────────

    @Nested
    @DisplayName("§4.3 EMPLOYEE / CORP_REP — 서류 심사형 등록")
    class ReviewTypeTest {

        @Test
        @DisplayName("EMPLOYEE 등록 요청은 SUBMITTED 상태로 저장")
        void register_employee_statusSubmitted() {
            String email = "master@test.com";
            Member member = memberWithPhone(email, "01011112222");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.existsByPhoneNumberAndStatusIn(any(), any())).willReturn(false);

            Callback saved = savedCallback(1L, "0215559999", CallbackRegisterType.EMPLOYEE, CallbackStatus.SUBMITTED);
            given(callbackRepository.save(any(Callback.class))).willReturn(saved);

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                "0215559999", CallbackRegisterType.EMPLOYEE, "임직원 내선번호"
            );

            CallbackDto.RegisterResponse resp = callbackService.register(email, req);

            assertThat(resp.status()).isEqualTo(CallbackStatus.SUBMITTED);
        }

        @Test
        @DisplayName("CORP_REP 등록 요청은 SUBMITTED 상태로 저장")
        void register_corpRep_statusSubmitted() {
            String email = "master@test.com";
            Member member = memberWithPhone(email, "01033334444");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.existsByPhoneNumberAndStatusIn(any(), any())).willReturn(false);

            Callback saved = savedCallback(1L, "025550000", CallbackRegisterType.CORP_REP, CallbackStatus.SUBMITTED);
            given(callbackRepository.save(any(Callback.class))).willReturn(saved);

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                "025550000", CallbackRegisterType.CORP_REP, "법인 대표번호"
            );

            CallbackDto.RegisterResponse resp = callbackService.register(email, req);

            assertThat(resp.status()).isEqualTo(CallbackStatus.SUBMITTED);
        }
    }

    // ── §4.4 삭제 ────────────────────────────────────────────────

    @Nested
    @DisplayName("§4.4 발신번호 삭제")
    class DeleteTest {

        @Test
        @DisplayName("본인 발신번호 삭제 — DELETED 상태 전이 + 로그 기록")
        void delete_ownCallback_success() {
            String email = "user@test.com";
            Member member = memberWithPhone(email, "01012345678");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

            Callback cb = savedCallback(1L, "01012345678", CallbackRegisterType.SELF_MOBILE, CallbackStatus.REGISTERED);
            given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));

            callbackService.delete(email, 1L);

            assertThat(cb.getStatus()).isEqualTo(CallbackStatus.DELETED);
            verify(callbackLogRepository).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 발신번호 삭제 시 EntityNotFoundException")
        void delete_notFound_throws() {
            String email = "user@test.com";
            Member member = memberWithPhone(email, "01012345678");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> callbackService.delete(email, 99L))
                .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── 중복 등록 방지 ────────────────────────────────────────────

    @Nested
    @DisplayName("활성 등록 1개 강제")
    class DuplicateTest {

        @Test
        @DisplayName("이미 활성 등록이 있으면 IllegalStateException")
        void register_duplicateActive_throws() {
            String email = "user@test.com";
            Member member = memberWithPhone(email, "01012345678");
            given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
            given(callbackRepository.existsByPhoneNumberAndStatusIn(any(), any())).willReturn(true);

            CallbackDto.RegisterRequest req = new CallbackDto.RegisterRequest(
                "01012345678", CallbackRegisterType.SELF_MOBILE, "중복 등록 시도"
            );

            assertThatThrownBy(() -> callbackService.register(email, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 등록");

            verify(callbackRepository, never()).save(any());
        }
    }

    // ── 목록 조회 ─────────────────────────────────────────────────

    @Test
    @DisplayName("회원의 발신번호 목록 조회")
    void list_returnsSummaries() {
        String email = "user@test.com";
        Member member = memberWithPhone(email, "01012345678");
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

        Callback cb1 = savedCallback(1L, "01012345678", CallbackRegisterType.SELF_MOBILE, CallbackStatus.REGISTERED);
        Callback cb2 = savedCallback(2L, "0215559999", CallbackRegisterType.EMPLOYEE, CallbackStatus.SUBMITTED);
        given(callbackRepository.findByMemberIdAndStatusNot(any(), any())).willReturn(List.of(cb1, cb2));

        List<CallbackDto.Summary> result = callbackService.list(email);

        assertThat(result).hasSize(2);
    }

    // ── 운영자 심사 (§12.2 — W-106 검토 큐) ─────────────────────

    @Nested
    @DisplayName("§12.2 운영자 발신번호 심사")
    class OperatorReviewTest {

        @Test
        @DisplayName("운영자 승인 — SUBMITTED → REGISTERED, 로그 기록")
        void approveCallback_success() {
            Callback cb = savedCallback(1L, "0215559999", CallbackRegisterType.EMPLOYEE, CallbackStatus.SUBMITTED);
            given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));

            callbackService.approveCallback(1L, 99L);

            assertThat(cb.getStatus()).isEqualTo(CallbackStatus.REGISTERED);
            verify(callbackLogRepository).save(any());
        }

        @Test
        @DisplayName("운영자 반려 — SUBMITTED → REJECTED, 사유 기록")
        void rejectCallback_success() {
            Callback cb = savedCallback(1L, "0215559999", CallbackRegisterType.EMPLOYEE, CallbackStatus.SUBMITTED);
            given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));

            callbackService.rejectCallback(1L, 99L, "서류 불충분");

            assertThat(cb.getStatus()).isEqualTo(CallbackStatus.REJECTED);
            verify(callbackLogRepository).save(any());
        }

        @Test
        @DisplayName("이미 REGISTERED 상태 승인 시도 — IllegalStateException")
        void approveCallback_alreadyRegistered_throws() {
            Callback cb = savedCallback(1L, "01012345678", CallbackRegisterType.SELF_MOBILE, CallbackStatus.REGISTERED);
            given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));

            assertThatThrownBy(() -> callbackService.approveCallback(1L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("심사 대기");
        }

        @Test
        @DisplayName("운영자 심사 큐 목록 — SUBMITTED + UNDER_REVIEW 반환")
        void listPendingReview_returnsSubmittedAndUnderReview() {
            Callback cb1 = savedCallback(1L, "0215559999", CallbackRegisterType.EMPLOYEE, CallbackStatus.SUBMITTED);
            Callback cb2 = savedCallback(2L, "025550000", CallbackRegisterType.CORP_REP, CallbackStatus.UNDER_REVIEW);
            given(callbackRepository.findByStatusIn(any())).willReturn(List.of(cb1, cb2));

            List<CallbackDto.Summary> result = callbackService.listPendingReview();

            assertThat(result).hasSize(2);
        }
    }
}
