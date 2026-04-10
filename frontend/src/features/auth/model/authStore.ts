import { create } from 'zustand';

interface Member {
  id: number;
  email: string;
  name: string;
}

interface AuthState {
  token: string | null;
  member: Member | null;
  isAuthenticated: boolean;
  setAuth: (token: string, member: Member) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: typeof window !== 'undefined' ? localStorage.getItem('token') : null,
  member: null,
  isAuthenticated: typeof window !== 'undefined' ? !!localStorage.getItem('token') : false,

  setAuth: (token, member) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('token', token);
    }
    set({ token, member, isAuthenticated: true });
  },

  clearAuth: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
    }
    set({ token: null, member: null, isAuthenticated: false });
  },
}));
