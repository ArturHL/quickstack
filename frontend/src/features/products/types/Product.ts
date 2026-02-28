export type ProductType = 'SIMPLE' | 'VARIANT' | 'COMBO'

export interface CategoryResponse {
  id: string
  tenantId: string
  name: string
  description: string | null
  imageUrl: string | null
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface ProductResponse {
  id: string
  tenantId: string
  categoryId: string | null
  categoryName: string | null
  name: string
  description: string | null
  sku: string | null
  basePrice: number
  costPrice: number | null
  productType: ProductType
  imageUrl: string | null
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface ProductPage {
  content: ProductResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ProductCreateRequest {
  name: string
  description?: string
  categoryId?: string
  sku?: string
  basePrice: number
  costPrice?: number
  productType: ProductType
  imageUrl?: string
  sortOrder?: number
}

export interface ProductUpdateRequest extends ProductCreateRequest {
  isActive?: boolean
}
