export interface BranchResponse {
  id: string
  tenantId: string
  name: string
  address: string | null
  city: string | null
  phone: string | null
  email: string | null
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface BranchCreateRequest {
  name: string
  address?: string
  city?: string
  phone?: string
  email?: string
}

export interface BranchUpdateRequest extends BranchCreateRequest {
  isActive?: boolean
}

export interface AreaCreateRequest {
  name: string
  description?: string
  sortOrder?: number
}

export interface TableCreateRequest {
  number: number
  name?: string
  capacity: number
  description?: string
  sortOrder?: number
}
