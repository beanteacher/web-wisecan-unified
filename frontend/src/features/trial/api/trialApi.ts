import ky from 'ky';

const trialApi = ky.create({
  prefixUrl: process.env.NEXT_PUBLIC_API_URL?.replace('/api/v1/', '/') || 'http://localhost:8080/',
  timeout: 10000,
});

export interface DummyContextSummary {
  dummyCallbackNumber: string;
  dummyApiKey: string;
  dummyBalance: number;
  dummySendHistoryJson: string;
  dummyKakaoTemplateJson: string;
  dummyRcsBrandJson: string;
}

export interface TrialSessionResponse {
  sessionToken: string;
  expiresAt: string;
  dummyContext: DummyContextSummary;
}

export interface TrialSendRequest {
  channel: 'SMS' | 'LMS' | 'MMS' | 'KAKAO' | 'RCS';
  recipientNumber: string;
  messageBody: string;
}

export interface TrialSendResponse {
  recordId: number;
  virtualResultCode: string;
  externalBlocked: boolean;
  message: string;
}

export interface BillingBlockedResponse {
  reason: string;
  message: string;
}

/** 체험 세션 발급 */
export async function issueTrialSession(): Promise<TrialSessionResponse> {
  return trialApi.post('trial/sessions').json<TrialSessionResponse>();
}

/** 체험 발송 (외부 송출 없음) */
export async function trialSend(
  sessionToken: string,
  request: TrialSendRequest,
): Promise<TrialSendResponse> {
  return trialApi
    .post(`trial/sessions/${sessionToken}/send`, { json: request })
    .json<TrialSendResponse>();
}

/** 체험 결제 차단 */
export async function blockTrialBilling(sessionToken: string): Promise<BillingBlockedResponse> {
  return trialApi
    .post(`trial/sessions/${sessionToken}/billing`)
    .json<BillingBlockedResponse>();
}

/** 체험 세션 종료 */
export async function closeTrialSession(sessionToken: string): Promise<void> {
  await trialApi.delete(`trial/sessions/${sessionToken}`);
}
