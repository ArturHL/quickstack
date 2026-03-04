import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate, useLocation } from 'react-router-dom'
import type { AxiosError } from 'axios'
import { authApi } from '../services/authApi'
import { useAuthStore } from '../stores/authStore'
import type {
  ApiError,
  AuthResponse,
  AuthUser,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types/auth'

const ROLE_ID_TO_CODE: Record<string, AuthUser['role']> = {
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa': 'OWNER',
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb': 'CASHIER',
  'cccccccc-cccc-cccc-cccc-cccccccccccc': 'KITCHEN',
}

export function useLogin() {
  const navigate = useNavigate()
  const location = useLocation()
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation<AuthResponse, AxiosError<ApiError>, LoginRequest>({
    mutationFn: (data) => authApi.login(data),
    onSuccess: (data) => {
      const user: AuthUser = {
        ...data.user,
        role: ROLE_ID_TO_CODE[data.user.roleId ?? ''] ?? data.user.role,
      }
      setAuth(data.accessToken, user)
      const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/'
      navigate(from, { replace: true })
    },
  })
}

export function useRegister() {
  const navigate = useNavigate()

  return useMutation<void, AxiosError<ApiError>, RegisterRequest>({
    mutationFn: (data) => authApi.register(data),
    onSuccess: () => {
      navigate('/login?registered=true')
    },
  })
}

export function useLogout() {
  const navigate = useNavigate()
  const clearAuth = useAuthStore((s) => s.clearAuth)

  const handleLoggedOut = () => {
    clearAuth()
    navigate('/login', { replace: true })
  }

  return useMutation<void, AxiosError<ApiError>>({
    mutationFn: () => authApi.logout(),
    onSuccess: handleLoggedOut,
    onError: handleLoggedOut,
  })
}

export function useForgotPassword() {
  return useMutation<void, AxiosError<ApiError>, ForgotPasswordRequest>({
    mutationFn: (data) => authApi.forgotPassword(data),
  })
}

export function useResetPassword() {
  return useMutation<void, AxiosError<ApiError>, ResetPasswordRequest>({
    mutationFn: (data) => authApi.resetPassword(data),
  })
}

export function useCurrentUser() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery({
    queryKey: ['currentUser'],
    queryFn: () => authApi.getMe(),
    enabled: isAuthenticated,
  })
}
