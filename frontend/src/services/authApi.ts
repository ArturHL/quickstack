import axiosInstance from '../utils/axiosInstance'
import type {
  AuthResponse,
  AuthUser,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
} from '../types/auth'

export const authApi = {
  login: (data: LoginRequest): Promise<AuthResponse> =>
    axiosInstance.post<AuthResponse>('/api/v1/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest): Promise<void> =>
    axiosInstance.post<void>('/api/v1/auth/register', data).then((r) => r.data),

  refreshToken: (): Promise<{ accessToken: string }> =>
    axiosInstance.post<{ accessToken: string }>('/api/v1/auth/refresh').then((r) => r.data),

  logout: (): Promise<void> =>
    axiosInstance.post<void>('/api/v1/auth/logout').then((r) => r.data),

  forgotPassword: (data: ForgotPasswordRequest): Promise<void> =>
    axiosInstance.post<void>('/api/v1/auth/forgot-password', data).then((r) => r.data),

  resetPassword: (data: ResetPasswordRequest): Promise<void> =>
    axiosInstance.post<void>('/api/v1/auth/reset-password', data).then((r) => r.data),

  getMe: (): Promise<AuthUser> =>
    axiosInstance.get<AuthUser>('/api/v1/auth/me').then((r) => r.data),
}
