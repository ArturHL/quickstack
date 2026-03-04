import axiosInstance from '../../../utils/axiosInstance'

export interface UserResponse {
  id: string
  email: string
  fullName: string
  phone?: string | null
  tenantId: string
  roleId: string
  roleCode: 'OWNER' | 'CASHIER' | 'KITCHEN'
  branchId?: string | null
  isActive: boolean
  createdAt: string
}

export interface UserPage {
  content: UserResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface UserCreateRequest {
  email: string
  fullName: string
  password: string
  roleId: string
  phone?: string
}

export interface UserUpdateRequest {
  fullName?: string
  phone?: string
  roleId?: string
}

export const ROLE_OPTIONS = [
  { id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', label: 'Cajero', code: 'CASHIER' },
  { id: 'cccccccc-cccc-cccc-cccc-cccccccccccc', label: 'Cocina', code: 'KITCHEN' },
  { id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', label: 'Dueño', code: 'OWNER' },
] as const

export const userAdminApi = {
  getUsers: (params: { search?: string; page?: number; size?: number } = {}): Promise<UserPage> =>
    axiosInstance
      .get<{ data: UserPage }>('/api/v1/users', { params })
      .then((r) => r.data.data),

  createUser: (body: UserCreateRequest): Promise<UserResponse> =>
    axiosInstance
      .post<{ data: UserResponse }>('/api/v1/users', body)
      .then((r) => r.data.data),

  updateUser: (id: string, body: UserUpdateRequest): Promise<UserResponse> =>
    axiosInstance
      .put<{ data: UserResponse }>(`/api/v1/users/${id}`, body)
      .then((r) => r.data.data),

  deleteUser: (id: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/users/${id}`).then(() => undefined),
}
