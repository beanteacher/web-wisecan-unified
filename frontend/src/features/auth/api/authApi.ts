import { api } from '@/shared/api/client';

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  name: string;
  role: string;
}

export interface RegisterResponse {
  accessToken: string;
  refreshToken: string;
  email: string;
  name: string;
  role: string;
}

export interface TermAgreementItem {
  termCode: string;
  agreed: boolean;
}

export interface ApiResponseWrapper<T> {
  success: boolean;
  data: T;
  message?: string;
}

export const authApi = {
  login: (email: string, password: string) =>
    api
      .post('auth/login', { json: { email, password } })
      .json<ApiResponseWrapper<LoginResponse>>()
      .then((res) => res.data),

  register: (
    email: string,
    password: string,
    name: string,
    phone: string | undefined,
    termAgreements: TermAgreementItem[]
  ) =>
    api
      .post('auth/register', { json: { email, password, name, phone, termAgreements } })
      .json<ApiResponseWrapper<RegisterResponse>>()
      .then((res) => res.data),

  logout: () => api.post('auth/logout').json<void>(),
};
