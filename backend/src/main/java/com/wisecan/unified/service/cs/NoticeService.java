package com.wisecan.unified.service.cs;

import com.wisecan.unified.domain.cs.Notice;
import com.wisecan.unified.dto.cs.NoticeDto;
import com.wisecan.unified.exception.EntityNotFoundException;
import com.wisecan.unified.repository.cs.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /** 회원용: 노출 공지 목록 (pinned 우선) */
    @Transactional(readOnly = true)
    public List<NoticeDto.Summary> listVisible() {
        return noticeRepository.findByVisibleTrueOrderByPinnedDescCreatedAtDesc()
                .stream().map(NoticeDto.Summary::from).toList();
    }

    /** 회원용: 공지 단건 조회 */
    @Transactional(readOnly = true)
    public NoticeDto.Detail detail(Long id) {
        return NoticeDto.Detail.from(findById(id));
    }

    /** 관리자: 전체 공지 목록 */
    @Transactional(readOnly = true)
    public Page<NoticeDto.Summary> listAll(Pageable pageable) {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc(pageable)
                .map(NoticeDto.Summary::from);
    }

    /** 관리자: 공지 등록 */
    @Transactional
    public NoticeDto.Detail create(Long adminId, NoticeDto.CreateRequest request) {
        Notice notice = Notice.builder()
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .pinned(request.pinned())
                .visible(request.visible())
                .authorAdminId(adminId)
                .build();
        return NoticeDto.Detail.from(noticeRepository.save(notice));
    }

    /** 관리자: 공지 수정 */
    @Transactional
    public NoticeDto.Detail update(Long id, NoticeDto.UpdateRequest request) {
        Notice notice = findById(id);
        notice.update(request.type(), request.title(), request.content(),
                request.pinned(), request.visible());
        return NoticeDto.Detail.from(notice);
    }

    /** 관리자: 공지 삭제 */
    @Transactional
    public void delete(Long id) {
        noticeRepository.delete(findById(id));
    }

    private Notice findById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("공지사항을 찾을 수 없습니다. id=" + id));
    }
}
