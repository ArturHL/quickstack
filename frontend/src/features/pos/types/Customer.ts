export interface CustomerResponse {
  id: string
  tenantId: string
  name: string | null
  phone: string | null
  email: string | null
  whatsapp: string | null
  addressLine1: string | null
  addressLine2: string | null
  city: string | null
  postalCode: string | null
  deliveryNotes: string | null
  totalOrders: number
  totalSpent: string
  lastOrderAt: string | null
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}

export interface CustomerPage {
  content: CustomerResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface CustomerCreateRequest {
  name?: string
  phone?: string
  email?: string
  whatsapp?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  postalCode?: string
  deliveryNotes?: string
}
