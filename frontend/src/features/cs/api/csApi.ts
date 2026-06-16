import { api } from '@/shared/api/client';

export type InquiryCategory = 'ACCOUNT' | 'SEND' | 'BILLING' | 'TECHNICAL' | 'ETC';
export type InquiryStatus = 'OPEN' | 'IN_PROGRESS' | 'ANSWERED' | 'CLOSED';
export type NoticeType = 'GENERAL' | 'MAINTENANCE' | 'IMPORTANT';

export interface InquirySummary {
  id: number;
  category: InquiryCategory;
  title: string;
  status: InquiryStatus;
  createdAt: string;
  answeredAt: string | null;
  answerMinutes: number;
}

export interface InquiryDetail extends InquirySummary {
  memberId: number;
  content: string;
  answerContent: string | null;
  answeredByAdminId: number | null;
  updatedAt: string;
  slaBreached: boolean;
}

export interface FaqItem {
  id: number;
  category: InquiryCategory;
  question: string;
  answer: string;
  sortOrder: number;
  visible: boolean;
  createdAt: string;
}

export interface NoticeSummary {
  id: number;
  type: NoticeType;
  title: string;
  pinned: boolean;
  visible: boolean;
  createdAt: string;
}

export interface NoticeDetail extends NoticeSummary {
  content: string;
  authorAdminId: number;
  updatedAt: string;
}

export interface ChatbotResponse {
  matchedQuestion: string | null;
  answer: string | null;
  matched: boolean;
  fallbackMessage: string | null;
}

export interface SlaStats {
  totalAnswered: number;
  withinSla: number;
  breachedSla: number;
  slaRate: number;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

async function unwrap<T>(promise: Promise<ApiResponse<T>>): Promise<T> {
  const res = await promise;
  return res.data;
}

export const csApi = {
  // ── 문의 ────────────────────────────────────────────────────
  createInquiry: (body: { category: InquiryCategory; title: string; content: string }) =>
    unwrap(api.post('cs/inquiries', { json: body }).json<ApiResponse<InquiryDetail>>()),

  myInquiries: (page = 0, size = 20) =>
    unwrap(api.get(`cs/inquiries/me?page=${page}&size=${size}`).json<ApiResponse<{ content: InquirySummary[]; totalElements: number }>>()),

  myInquiryDetail: (id: number) =>
    unwrap(api.get(`cs/inquiries/me/${id}`).json<ApiResponse<InquiryDetail>>()),

  closeInquiry: (id: number) =>
    unwrap(api.patch(`cs/inquiries/me/${id}/close`).json<ApiResponse<null>>()),

  // ── FAQ ─────────────────────────────────────────────────────
  listFaqs: (category?: InquiryCategory) => {
    const url = category ? `cs/faqs?category=${category}` : 'cs/faqs';
    return unwrap(api.get(url).json<ApiResponse<FaqItem[]>>());
  },

  // ── 챗봇 ────────────────────────────────────────────────────
  chatbotQuery: (question: string) =>
    unwrap(api.post('cs/chatbot/query', { json: { question } }).json<ApiResponse<ChatbotResponse>>()),

  // ── 공지사항 ─────────────────────────────────────────────────
  listNotices: () =>
    unwrap(api.get('cs/notices').json<ApiResponse<NoticeSummary[]>>()),

  noticeDetail: (id: number) =>
    unwrap(api.get(`cs/notices/${id}`).json<ApiResponse<NoticeDetail>>()),
};
