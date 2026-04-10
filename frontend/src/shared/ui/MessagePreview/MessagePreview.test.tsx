import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MessagePreview } from './MessagePreview';

describe('MessagePreview', () => {
  it('variant 없을 때 Empty 상태 placeholder 를 렌더한다', () => {
    render(<MessagePreview />);
    expect(screen.getByText('미리보기')).toBeInTheDocument();
    expect(screen.getByText(/수신자와 본문을 입력하면/)).toBeInTheDocument();
  });

  it('recipient/content 모두 비어있을 때 Empty 상태를 렌더한다', () => {
    render(<MessagePreview variant="sms" recipient="" content="" />);
    expect(screen.getByText(/수신자와 본문을 입력하면/)).toBeInTheDocument();
  });

  it('SMS variant: 짧은 본문이 말풍선에 표시된다', () => {
    render(
      <MessagePreview variant="sms" recipient="01012345678" content="안녕하세요 테스트 메시지" />,
    );
    expect(screen.getByText('안녕하세요 테스트 메시지')).toBeInTheDocument();
    expect(screen.getByText('01012345678')).toBeInTheDocument();
    expect(screen.getByText('SMS')).toBeInTheDocument();
  });

  it('LMS variant: 긴 본문(여러 줄)이 렌더된다', () => {
    const longContent =
      '[Wisecan 알림] 안녕하세요 김OO 고객님.\n요청하신 상세 내용을 아래와 같이 안내드립니다.\n\n1) 항목 A — 결제 완료\n2) 항목 B — 배송 준비 중\n3) 항목 C — 고객센터 문의 가능';
    render(<MessagePreview variant="lms" recipient="01099998888" content={longContent} />);
    // whitespace-pre-wrap p 요소는 개행이 포함되어 getByText 정확 매칭이 안 되므로 부분 텍스트로 확인
    expect(screen.getByText(/Wisecan 알림/)).toBeInTheDocument();
    expect(screen.getByText(/항목 A/)).toBeInTheDocument();
    expect(screen.getByText('LMS')).toBeInTheDocument();
    expect(screen.getByText(/스크롤하여 더 보기/)).toBeInTheDocument();
  });

  it('MMS variant: imageUrl 전달 시 img 와 imageName 을 렌더한다', () => {
    render(
      <MessagePreview
        variant="mms"
        recipient="01011112222"
        content="이벤트 쿠폰입니다."
        imageUrl="blob:http://localhost/test-image"
        imageName="event-banner.jpg"
      />,
    );
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', 'blob:http://localhost/test-image');
    expect(img).toHaveAttribute('alt', 'event-banner.jpg');
    expect(screen.getByText('event-banner.jpg')).toBeInTheDocument();
  });
});
