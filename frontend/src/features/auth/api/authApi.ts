import { api } from '@/shared/api/client';

export interface LoginResponse {
  accessToken: string;
  member: {
    id: number;
    email: string;
    name: string;
  };
}

export interface RegisterResponse {
  id: number;
  email: string;
  name: string;
}

export const authApi = {
  login: (email: string, password: string) =>
    api.post('auth/login', { json: { email, password } }).json<LoginResponse>(),

  register: (email: string, password: string, name: string) =>
    api.post('auth/register', { json: { email, password, name } }).json<RegisterResponse>(),
};
