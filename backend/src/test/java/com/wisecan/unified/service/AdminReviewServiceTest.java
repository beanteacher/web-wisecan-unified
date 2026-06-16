package com.wisecan.unified.service;

import com.wisecan.unified.domain.ApiKey;
import com.wisecan.unified.domain.ApiKeyStatus;
import com.wisecan.unified.domain.ApiKeyType;
import com.wisecan.unified.domain.BusinessApplication;
import com.wisecan.unified.domain.Company;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.sendernumber.Callback;
import com.wisecan.unified.domain.sendernumber.CallbackRegisterType;
import com.wisecan.unified.domain.sendernumber.CallbackStatus;
import com.wisecan.unified.dto.AdminReviewDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.ApiKeyRepository;
import com.wisecan.unified.repository.BusinessApplicationRepository;
import com.wisecan.unified.repository.CompanyRepository;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.sendernumber.CallbackRepository;
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

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock private BusinessApplicationRepository businessApplicationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private CallbackRepository callbackRepository;
    @Mock private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private AdminReviewService adminReviewService;

    // ── §12.1 사업자 전환 심사 ──────────────────────────────────────────────

    @Test
    @DisplayName("사업자 전환 심사 큐 목록 — SUBMITTED+UNDER_REVIEW 반환")
    void listBusinessApplicationQueue_returnsQueueItems() {
        BusinessApplication app = BusinessApplication.builder()
            .memberId(1L)
            .status("SUBMITTED")
            .bizNumber("1234567890")
            .companyName("테스트회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();

        Member member = Member.builder()
            .email("admin@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        given(businessApplicationRepository.findByStatusIn(List.of("SUBMITTED", "UNDER_REVIEW")))
            .willReturn(List.of(app));
        given(memberRepository.findAllById(any())).willReturn(List.of(member));

        List<AdminReviewDto.BusinessApplicationSummary> result =
            adminReviewService.listBusinessApplicationQueue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("테스트회사");
    }

    @Test
    @DisplayName("사업자 전환 심사 시작 — SUBMITTED → UNDER_REVIEW")
    void startReview_submittedApp_changesStatusToUnderReview() {
        BusinessApplication app = BusinessApplication.builder()
            .memberId(1L)
            .status("SUBMITTED")
            .bizNumber("1234567890")
            .companyName("테스트회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();

        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        given(businessApplicationRepository.findById(1L)).willReturn(Optional.of(app));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));

        adminReviewService.startReview(1L);

        assertThat(app.getStatus()).isEqualTo("UNDER_REVIEW");
    }

    @Test
    @DisplayName("사업자 전환 심사 시작 — SUBMITTED 아닌 상태면 IllegalStateException")
    void startReview_notSubmitted_throwsException() {
        BusinessApplication app = BusinessApplication.builder()
            .memberId(1L)
            .status("APPROVED")
            .bizNumber("1234567890")
            .companyName("테스트회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();

        given(businessApplicationRepository.findById(1L)).willReturn(Optional.of(app));

        assertThatThrownBy(() -> adminReviewService.startReview(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SUBMITTED");
    }

    @Test
    @DisplayName("사업자 전환 승인 — Company 생성 및 Member COMPANY_MASTER 전이")
    void approveBusinessApplication_success() {
        BusinessApplication app = BusinessApplication.builder()
            .memberId(1L)
            .status("UNDER_REVIEW")
            .bizNumber("1234567890")
            .companyName("테스트회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();

        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        Company company = Company.builder()
            .name("테스트회사")
            .bizNumber("1234567890")
            .billingMode("PREPAID")
            .status("ACTIVE")
            .build();

        given(businessApplicationRepository.findById(1L)).willReturn(Optional.of(app));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(companyRepository.save(any(Company.class))).willReturn(company);

        AdminReviewDto.BusinessApplicationDetail result =
            adminReviewService.approveBusinessApplication(1L, 99L);

        assertThat(app.getStatus()).isEqualTo("APPROVED");
        assertThat(member.getRole()).isEqualTo(MemberRole.COMPANY_MASTER);
        assertThat(result.companyName()).isEqualTo("테스트회사");
    }

    @Test
    @DisplayName("사업자 전환 반려 — REJECTED 전이 + 사유 저장")
    void rejectBusinessApplication_success() {
        BusinessApplication app = BusinessApplication.builder()
            .memberId(1L)
            .status("UNDER_REVIEW")
            .bizNumber("1234567890")
            .companyName("테스트회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();

        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        given(businessApplicationRepository.findById(1L)).willReturn(Optional.of(app));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));

        AdminReviewDto.BusinessApplicationDetail result =
            adminReviewService.rejectBusinessApplication(1L, 99L, "서류 위조 의심");

        assertThat(app.getStatus()).isEqualTo("REJECTED");
        assertThat(app.getRejectReason()).isEqualTo("서류 위조 의심");
    }

    // ── §12.2 발신번호 심사 ─────────────────────────────────────────────────

    @Test
    @DisplayName("발신번호 심사 큐 목록 — SUBMITTED+UNDER_REVIEW 반환")
    void listCallbackQueue_returnsQueueItems() {
        Callback cb = Callback.builder()
            .memberId(1L)
            .phoneNumber("01012345678")
            .registerType(CallbackRegisterType.EMPLOYEE)
            .description("직원 번호")
            .status(CallbackStatus.SUBMITTED)
            .build();

        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        given(callbackRepository.findByStatusIn(
            List.of(CallbackStatus.SUBMITTED, CallbackStatus.UNDER_REVIEW)))
            .willReturn(List.of(cb));
        given(memberRepository.findAllById(any())).willReturn(List.of(member));

        List<AdminReviewDto.CallbackReviewSummary> result =
            adminReviewService.listCallbackQueue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).phoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("발신번호 승인 — SUBMITTED → REGISTERED")
    void approveCallback_success() {
        Callback cb = Callback.builder()
            .memberId(1L)
            .phoneNumber("01012345678")
            .registerType(CallbackRegisterType.EMPLOYEE)
            .description("직원 번호")
            .status(CallbackStatus.SUBMITTED)
            .build();

        given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));
        given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

        adminReviewService.approveCallback(1L);

        assertThat(cb.getStatus()).isEqualTo(CallbackStatus.REGISTERED);
    }

    @Test
    @DisplayName("발신번호 반려 — SUBMITTED → REJECTED + 사유 저장")
    void rejectCallback_success() {
        Callback cb = Callback.builder()
            .memberId(1L)
            .phoneNumber("01012345678")
            .registerType(CallbackRegisterType.EMPLOYEE)
            .description("직원 번호")
            .status(CallbackStatus.SUBMITTED)
            .build();

        given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));
        given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

        adminReviewService.rejectCallback(1L, "서류 불일치");

        assertThat(cb.getStatus()).isEqualTo(CallbackStatus.REJECTED);
        assertThat(cb.getRejectReason()).isEqualTo("서류 불일치");
    }

    @Test
    @DisplayName("발신번호 심사 — 이미 REGISTERED 상태면 IllegalStateException")
    void approveCallback_alreadyRegistered_throwsException() {
        Callback cb = Callback.builder()
            .memberId(1L)
            .phoneNumber("01012345678")
            .registerType(CallbackRegisterType.EMPLOYEE)
            .description("직원 번호")
            .status(CallbackStatus.REGISTERED)
            .build();

        given(callbackRepository.findById(1L)).willReturn(Optional.of(cb));

        assertThatThrownBy(() -> adminReviewService.approveCallback(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("심사 대기");
    }

    // ── §12.6 API Key 검토 큐 ────────────────────────────────────────────────

    @Test
    @DisplayName("API Key 검토 큐 목록 — PENDING_REVIEW 상태 반환")
    void listApiKeyQueue_returnsPendingKeys() {
        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("운영 키")
            .keyPrefix("wc_12345")
            .keyHash("somehash")
            .status(ApiKeyStatus.PENDING_REVIEW)
            .keyType(ApiKeyType.PRODUCTION)
            .build();

        given(apiKeyRepository.findByStatus(ApiKeyStatus.PENDING_REVIEW)).willReturn(List.of(key));

        List<AdminReviewDto.ApiKeyReviewSummary> result = adminReviewService.listApiKeyQueue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).keyName()).isEqualTo("운영 키");
        assertThat(result.get(0).keyType()).isEqualTo("PRODUCTION");
    }

    @Test
    @DisplayName("API Key 승인 — PENDING_REVIEW → ACTIVE")
    void approveApiKey_success() {
        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("운영 키")
            .keyPrefix("wc_12345")
            .keyHash("somehash")
            .status(ApiKeyStatus.PENDING_REVIEW)
            .keyType(ApiKeyType.PRODUCTION)
            .build();

        given(apiKeyRepository.findById(1L)).willReturn(Optional.of(key));

        adminReviewService.approveApiKey(1L);

        assertThat(key.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
    }

    @Test
    @DisplayName("API Key 반려 — PENDING_REVIEW → REVOKED")
    void rejectApiKey_success() {
        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("운영 키")
            .keyPrefix("wc_12345")
            .keyHash("somehash")
            .status(ApiKeyStatus.PENDING_REVIEW)
            .keyType(ApiKeyType.PRODUCTION)
            .build();

        given(apiKeyRepository.findById(1L)).willReturn(Optional.of(key));

        adminReviewService.rejectApiKey(1L, "허가되지 않은 용도");

        assertThat(key.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    @DisplayName("API Key 승인 — ACTIVE 상태면 IllegalStateException")
    void approveApiKey_alreadyActive_throwsException() {
        Member member = Member.builder()
            .email("user@test.com")
            .password("pw")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();

        ApiKey key = ApiKey.builder()
            .member(member)
            .keyName("운영 키")
            .keyPrefix("wc_12345")
            .keyHash("somehash")
            .status(ApiKeyStatus.ACTIVE)
            .keyType(ApiKeyType.PRODUCTION)
            .build();

        given(apiKeyRepository.findById(1L)).willReturn(Optional.of(key));

        assertThatThrownBy(() -> adminReviewService.approveApiKey(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING_REVIEW");
    }

    @Test
    @DisplayName("존재하지 않는 신청 조회 — EntityNotFoundException")
    void getBusinessApplicationDetail_notFound_throwsException() {
        given(businessApplicationRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminReviewService.getBusinessApplicationDetail(999L))
            .isInstanceOf(EntityNotFoundException.class);
    }
}
