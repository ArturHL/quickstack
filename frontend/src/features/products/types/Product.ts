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

export interface VariantResponse {
  id: string
  productId: string
  tenantId: string
  name: string
  priceAdjustment: number
  effectivePrice: number
  isDefault: boolean
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface ModifierResponse {
  id: string
  groupId: string
  tenantId: string
  name: string
  priceAdjustment: number
  isDefault: boolean
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface ModifierGroupResponse {
  id: string
  productId: string
  tenantId: string
  name: string
  minSelections: number
  maxSelections: number | null
  isRequired: boolean
  sortOrder: number
  isActive: boolean
  modifiers: ModifierResponse[]
  createdAt: string
  updatedAt: string
}

export interface ModifierGroupCreateRequest {
  name: string
  minSelections: number
  maxSelections?: number | null
  isRequired: boolean
}

export type ModifierGroupUpdateRequest = ModifierGroupCreateRequest

export interface ModifierCreateRequest {
  name: string
  priceAdjustment: number
  isDefault?: boolean
}

export type ModifierUpdateRequest = ModifierCreateRequest

export interface ComboItemResponse {
  productId: string
  productName: string
  quantity: number
}

export interface ComboResponse {
  id: string
  tenantId: string
  name: string
  description: string | null
  imageUrl: string | null
  price: number
  sortOrder: number
  isActive: boolean
  items: ComboItemResponse[]
  createdAt: string
  updatedAt: string
}

export interface ComboPage {
  content: ComboResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ComboCreateRequest {
  name: string
  description?: string
  price: number
  sortOrder?: number
  items: { productId: string; quantity: number }[]
}

export interface ComboUpdateRequest extends ComboCreateRequest {
  isActive?: boolean
}

export interface VariantCreateRequest {
  name: string
  priceAdjustment: number
  isDefault?: boolean
  sortOrder?: number
}

export interface VariantUpdateRequest {
  name: string
  priceAdjustment: number
  isDefault?: boolean
  sortOrder?: number
}
