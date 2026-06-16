import { api } from '@/shared/api/client';

export interface BusinessApplicationRequest {
  bizNumber: string;
  corpNumber?: string;
  companyName: string;
  ceoName: string;
  ceoPhone?: string;
}

export interface BusinessApplicationResponse {
  applicationId: number;
  status: string;
  companyName: string;
  bizNumber: string;
  rejectReason?: string;
}

interface ApiResponseWrapper<T> {
  success: boolean;
  data: T;
  message?: string;
}

export const businessApi = {
  submit: (req: BusinessApplicationRequest) =>
    api
      .post('business-applications', { json: req })
      .json<ApiResponseWrapper<BusinessApplicationResponse>>()
      .then((res) => res.data),

  getStatus: () =>
    api
      .get('business-applications/status')
      .json<ApiResponseWrapper<BusinessApplicationResponse>>()
      .then((res) => res.data),
};
