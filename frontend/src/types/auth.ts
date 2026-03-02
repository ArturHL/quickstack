export interface AuthUser {
  id: string
  email: string
  fullName: string
  role: 'OWNER' | 'MANAGER' | 'CASHIER' | 'WAITER' | 'KITCHEN'
  roleId?: string
  branchId?: string | null
  tenantId: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface LoginRequest {
  email: string
  password: string
  tenantId: string
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  tenantId: string
  roleId: string
  phone?: string
}

export interface ForgotPasswordRequest {
  email: string
  tenantId: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
}

export interface ApiError {
  error: string
  message: string
}
