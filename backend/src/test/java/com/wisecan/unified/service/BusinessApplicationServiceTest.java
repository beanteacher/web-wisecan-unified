package com.wisecan.unified.service;

import com.wisecan.unified.domain.BusinessApplication;
import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.dto.BusinessApplicationDto;
import com.wisecan.unified.repository.BusinessApplicationRepository;
import com.wisecan.unified.repository.MemberRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BusinessApplicationServiceTest {

    @Mock
    private BusinessApplicationRepository businessApplicationRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BusinessApplicationService businessApplicationService;

    private Member sampleMember() {
        return Member.builder()
            .email("user@test.com")
            .password("encodedPassword")
            .name("홍길동")
            .role(MemberRole.MEMBER)
            .status(MemberStatus.ACTIVE)
            .build();
    }

    @Test
    @DisplayName("사업자 전환 신청 성공 — SUBMITTED 상태로 저장")
    void submit_success() {
        BusinessApplicationDto.SubmitRequest request = new BusinessApplicationDto.SubmitRequest(
            "123-45-67890", null, "위즈캔 주식회사", "홍길동", "010-1234-5678"
        );
        Member member = sampleMember();

        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(businessApplicationRepository.existsByMemberIdAndStatusIn(any(), any())).willReturn(false);

        BusinessApplication saved = BusinessApplication.builder()
            .memberId(1L)
            .status("SUBMITTED")
            .bizNumber("123-45-67890")
            .companyName("위즈캔 주식회사")
            .ceoName("홍길동")
            .ceoPhone("010-1234-5678")
            .build();
        given(businessApplicationRepository.save(any(BusinessApplication.class))).willReturn(saved);

        BusinessApplicationDto.StatusResponse response =
            businessApplicationService.submit("user@test.com", request);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        assertThat(response.companyName()).isEqualTo("위즈캔 주식회사");
        verify(businessApplicationRepository).save(any(BusinessApplication.class));
    }

    @Test
    @DisplayName("이미 심사 중인 신청이 있으면 IllegalStateException 발생")
    void submit_alreadyPending_throwsException() {
        BusinessApplicationDto.SubmitRequest request = new BusinessApplicationDto.SubmitRequest(
            "123-45-67890", null, "위즈캔 주식회사", "홍길동", "010-1234-5678"
        );
        Member member = sampleMember();

        given(memberRepository.findByEmail("user@test.com")).willReturn(Optional.of(member));
        given(businessApplicationRepository.existsByMemberIdAndStatusIn(
            any(), any())).willReturn(true);

        assertThatThrownBy(() -> businessApplicationService.submit("user@test.com", request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 심사 중인 신청");
    }

    @Test
    @DisplayName("회원 미존재 시 RuntimeException 발생")
    void submit_memberNotFound_throwsException() {
        BusinessApplicationDto.SubmitRequest request = new BusinessApplicationDto.SubmitRequest(
            "123-45-67890", null, "위즈캔 주식회사", "홍길동", null
        );

        given(memberRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessApplicationService.submit("none@test.com", request))
            .isInstanceOf(RuntimeException.class);
    }
}
