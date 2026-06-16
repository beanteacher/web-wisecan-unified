/**
 * 카카오·RCS 템플릿/브랜드 도메인 타입 (W-305).
 * 백엔드 TemplateDto 와 1:1 대응.
 * 중계사 식별자(kko_profile_no 등)는 포함하지 않는다 (INV-02).
 */

// ── 카카오 ────────────────────────────────────────────────────────

export type KakaoInspectionStatus = 'REG' | 'REQ' | 'APR' | 'REJ';
export type KakaoTemplateStatus = 'A' | 'S' | 'N' | 'D';

export interface KakaoTemplate {
  templateCode: string;
  templateName: string;
  templateContent: string;
  inspectionStatus: KakaoInspectionStatus | null;
  templateStatus: KakaoTemplateStatus | null;
  messageType: string;
  categoryCode: string;
  buttons: string | null;
  securityFlag: boolean;
  sendable: boolean;
}

export interface KakaoRegisterRequest {
  templateName: string;
  templateContent: string;
  messageType: string;
  categoryCodeM: string;
  categoryCodeS: string;
  buttons?: string;
  securityFlag: boolean;
}

// ── RCS ───────────────────────────────────────────────────────────

export interface RcsTemplate {
  messagebaseId: string;
  templateName: string;
  brandId: string;
  usageStatus: string | null;
  approvalResult: string | null;
  approvalReason: string | null;
  productCode: string;
  spec: string;
  cardType: string;
  inputText: string;
  sendable: boolean;
}

// ── 이관 처리 큐 ─────────────────────────────────────────────────

export type TransferStatus =
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELLED';

export interface TransferItem {
  id: number;
  memberId: number;
  sourceTemplateCode: string;
  status: TransferStatus;
  requestedAt: string;
}

export interface TransferDetail extends TransferItem {
  kkoProfileNo: number | null;
  reason: string | null;
  rejectReason: string | null;
  operatorId: number | null;
  resolvedAt: string | null;
}

export interface TransferRequest {
  sourceTemplateCode: string;
  kkoProfileNo?: number;
  reason?: string;
}
