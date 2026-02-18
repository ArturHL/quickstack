import { create } from 'zustand'
import type { AuthUser } from '../types/auth'

interface AuthState {
  accessToken: string | null
  user: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
}

interface AuthActions {
  setAuth: (token: string, user: AuthUser) => void
  clearAuth: () => void
  setLoading: (loading: boolean) => void
}

const initialState: AuthState = {
  accessToken: null,
  user: null,
  isAuthenticated: false,
  isLoading: false,
}

export const useAuthStore = create<AuthState & AuthActions>()((set) => ({
  ...initialState,

  setAuth: (token, user) =>
    set({ accessToken: token, user, isAuthenticated: true, isLoading: false }),

  clearAuth: () => set({ ...initialState }),

  setLoading: (loading) => set({ isLoading: loading }),
}))
