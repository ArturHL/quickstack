import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import type { AxiosError } from 'axios'
import { authApi } from '../services/authApi'
import { useAuthStore } from '../stores/authStore'
import type {
  ApiError,
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types/auth'

export function useLogin() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation<AuthResponse, AxiosError<ApiError>, LoginRequest>({
    mutationFn: (data) => authApi.login(data),
    onSuccess: (data) => {
      setAuth(data.accessToken, data.user)
      navigate('/dashboard', { replace: true })
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
