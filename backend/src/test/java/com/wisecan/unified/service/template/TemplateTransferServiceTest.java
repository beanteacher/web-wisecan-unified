package com.wisecan.unified.service.template;

import com.wisecan.unified.domain.Member;
import com.wisecan.unified.domain.MemberRole;
import com.wisecan.unified.domain.MemberStatus;
import com.wisecan.unified.domain.template.TemplateTransferQueue;
import com.wisecan.unified.domain.template.TemplateTransferStatus;
import com.wisecan.unified.dto.template.TemplateDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.MemberRepository;
import com.wisecan.unified.repository.template.TemplateTransferQueueRepository;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateTransferService 단위 테스트")
class TemplateTransferServiceTest {

    @Mock
    private TemplateTransferQueueRepository transferQueueRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TemplateTransferService templateTransferService;

    private static final String EMAIL = "test@wisecan.com";
    private static final Long MEMBER_ID = 1L;
    private static final Long OPERATOR_ID = 99L;

    private Member member() {
        return Member.builder()
                .email(EMAIL)
                .password("hashed")
                .name("홍길동")
                .phone("01012345678")
                .role(MemberRole.MEMBER)
                .status(MemberStatus.ACTIVE)
                .build();
    }

    private TemplateTransferQueue pendingQueue(Long id) {
        TemplateTransferQueue queue = TemplateTransferQueue.builder()
                .memberId(MEMBER_ID)
                .sourceTemplateCode("tmpl_src_001")
                .kkoProfileNo(1001)
                .reason("SMS17에서 이관합니다")
                .build();
        // reflection 없이 id를 직접 주입할 수 없으므로 save 이후 반환값으로 처리
        return queue;
    }

    // ── 이관 신청 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("requestTransfer()")
    class RequestTransferTest {

        @Test
        @DisplayName("정상 이관 신청 → PENDING 상태로 저장")
        void requestTransfer_success_savesPending() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(transferQueueRepository.existsByMemberIdAndSourceTemplateCodeAndStatusIn(
                    any(), any(), any())).willReturn(false);
            TemplateTransferQueue saved = pendingQueue(1L);
            given(transferQueueRepository.save(any())).willReturn(saved);

            TemplateDto.TransferRequest request = new TemplateDto.TransferRequest(
                    "tmpl_src_001", 1001, "SMS17에서 이관합니다");

            TemplateDto.TransferResponse response =
                    templateTransferService.requestTransfer(EMAIL, request);

            assertThat(response.status()).isEqualTo(TemplateTransferStatus.PENDING);
            assertThat(response.sourceTemplateCode()).isEqualTo("tmpl_src_001");
            verify(transferQueueRepository).save(any(TemplateTransferQueue.class));
        }

        @Test
        @DisplayName("진행 중인 이관 신청 중복 → IllegalStateException")
        void requestTransfer_duplicate_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            given(transferQueueRepository.existsByMemberIdAndSourceTemplateCodeAndStatusIn(
                    any(), any(), any())).willReturn(true);

            TemplateDto.TransferRequest request = new TemplateDto.TransferRequest(
                    "tmpl_src_001", 1001, "중복 신청");

            assertThatThrownBy(() -> templateTransferService.requestTransfer(EMAIL, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 진행 중인 이관 신청");
        }

        @Test
        @DisplayName("회원 없음 → EntityNotFoundException")
        void requestTransfer_memberNotFound_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

            assertThatThrownBy(() -> templateTransferService.requestTransfer(EMAIL,
                    new TemplateDto.TransferRequest("tmpl", null, null)))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── 이관 취소 ─────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelTransfer()")
    class CancelTransferTest {

        @Test
        @DisplayName("PENDING 상태 본인 신청 → CANCELLED 전이")
        void cancelTransfer_pending_succeeds() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            TemplateTransferQueue queue = pendingQueue(1L);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            templateTransferService.cancelTransfer(EMAIL, 1L);

            assertThat(queue.getStatus()).isEqualTo(TemplateTransferStatus.CANCELLED);
        }

        @Test
        @DisplayName("IN_PROGRESS 상태 → IllegalStateException")
        void cancelTransfer_inProgress_throws() {
            given(memberRepository.findByEmail(EMAIL)).willReturn(Optional.of(member()));
            TemplateTransferQueue queue = pendingQueue(1L);
            queue.startProcess(OPERATOR_ID);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            assertThatThrownBy(() -> templateTransferService.cancelTransfer(EMAIL, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    // ── 운영자 처리 ───────────────────────────────────────────────

    @Nested
    @DisplayName("processTransfer()")
    class ProcessTransferTest {

        @Test
        @DisplayName("승인 처리 → COMPLETED 전이")
        void processTransfer_approve_completed() {
            TemplateTransferQueue queue = pendingQueue(1L);
            queue.startProcess(OPERATOR_ID);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            TemplateDto.TransferProcessRequest request =
                    new TemplateDto.TransferProcessRequest(true, null);

            TemplateDto.TransferDetail result =
                    templateTransferService.processTransfer(1L, OPERATOR_ID, request);

            assertThat(result.status()).isEqualTo(TemplateTransferStatus.COMPLETED);
        }

        @Test
        @DisplayName("거부 처리 → REJECTED 전이, 사유 저장")
        void processTransfer_reject_rejected() {
            TemplateTransferQueue queue = pendingQueue(1L);
            queue.startProcess(OPERATOR_ID);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            TemplateDto.TransferProcessRequest request =
                    new TemplateDto.TransferProcessRequest(false, "템플릿 형식 불일치");

            TemplateDto.TransferDetail result =
                    templateTransferService.processTransfer(1L, OPERATOR_ID, request);

            assertThat(result.status()).isEqualTo(TemplateTransferStatus.REJECTED);
            assertThat(result.rejectReason()).isEqualTo("템플릿 형식 불일치");
        }

        @Test
        @DisplayName("거부 처리 시 사유 없음 → IllegalArgumentException")
        void processTransfer_reject_noReason_throws() {
            TemplateTransferQueue queue = pendingQueue(1L);
            queue.startProcess(OPERATOR_ID);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            TemplateDto.TransferProcessRequest request =
                    new TemplateDto.TransferProcessRequest(false, "");

            assertThatThrownBy(() ->
                    templateTransferService.processTransfer(1L, OPERATOR_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사유");
        }

        @Test
        @DisplayName("PENDING 상태에서 processTransfer → IllegalStateException")
        void processTransfer_notInProgress_throws() {
            TemplateTransferQueue queue = pendingQueue(1L);
            given(transferQueueRepository.findById(1L)).willReturn(Optional.of(queue));

            TemplateDto.TransferProcessRequest request =
                    new TemplateDto.TransferProcessRequest(true, null);

            assertThatThrownBy(() ->
                    templateTransferService.processTransfer(1L, OPERATOR_ID, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("IN_PROGRESS");
        }

        @Test
        @DisplayName("존재하지 않는 이관 신청 → EntityNotFoundException")
        void processTransfer_notFound_throws() {
            given(transferQueueRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    templateTransferService.processTransfer(999L, OPERATOR_ID,
                            new TemplateDto.TransferProcessRequest(true, null)))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    // ── 운영자 큐 조회 ────────────────────────────────────────────

    @Nested
    @DisplayName("listPendingQueue()")
    class ListPendingQueueTest {

        @Test
        @DisplayName("PENDING 목록 반환")
        void listPendingQueue_returnsPendingItems() {
            TemplateTransferQueue queue = pendingQueue(1L);
            given(transferQueueRepository.findByStatusOrderByRequestedAtAsc(
                    TemplateTransferStatus.PENDING))
                    .willReturn(List.of(queue));

            List<TemplateDto.TransferDetail> result = templateTransferService.listPendingQueue();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(TemplateTransferStatus.PENDING);
        }
    }
}
