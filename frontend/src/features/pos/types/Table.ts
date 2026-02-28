export type TableStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'MAINTENANCE'

export interface AreaResponse {
  id: string
  tenantId: string
  branchId: string
  name: string
  description: string | null
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface TableResponse {
  id: string
  tenantId: string
  areaId: string
  number: number
  name: string | null
  capacity: number
  status: TableStatus
  sortOrder: number
  positionX: number | null
  positionY: number | null
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}
