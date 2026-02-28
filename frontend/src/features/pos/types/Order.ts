import type { ServiceType } from './Cart'

// ─── Request Types ─────────────────────────────────────────────────────────────

export interface OrderItemModifierRequest {
  modifierId: string
  modifierName: string
  priceAdjustment: number
  quantity: number
}

export interface OrderItemRequest {
  productId?: string
  variantId?: string
  comboId?: string
  productName: string
  variantName?: string
  quantity: number
  unitPrice: number
  modifiers: OrderItemModifierRequest[]
  notes?: string
}

export interface OrderCreateRequest {
  branchId: string
  serviceType: ServiceType
  tableId?: string
  customerId?: string
  items: OrderItemRequest[]
  notes?: string
  kitchenNotes?: string
}

export interface PaymentRequest {
  orderId: string
  paymentMethod: PaymentMethod
  amount: number
  notes?: string
}

// ─── Response Types ────────────────────────────────────────────────────────────

export type OrderStatus = 'PENDING' | 'IN_PROGRESS' | 'READY' | 'DELIVERED' | 'COMPLETED' | 'CANCELLED'
export type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER'

export interface OrderItemModifierResponse {
  id: string
  modifierId: string
  modifierName: string
  priceAdjustment: number
  quantity: number
}

export interface OrderItemResponse {
  id: string
  tenantId: string
  orderId: string
  productId: string | null
  variantId: string | null
  comboId: string | null
  productName: string
  variantName: string | null
  quantity: number
  unitPrice: number
  modifiersTotal: number
  lineTotal: number
  kdsStatus: string
  kdsSentAt: string | null
  kdsReadyAt: string | null
  notes: string | null
  sortOrder: number
  modifiers: OrderItemModifierResponse[]
  createdAt: string
  updatedAt: string
}

export interface OrderResponse {
  id: string
  tenantId: string
  branchId: string
  tableId: string | null
  customerId: string | null
  orderNumber: string
  dailySequence: number
  serviceType: ServiceType
  statusId: string
  subtotal: number
  taxRate: number
  tax: number
  discount: number
  total: number
  source: string
  notes: string | null
  kitchenNotes: string | null
  openedAt: string
  closedAt: string | null
  createdBy: string
  updatedBy: string
  createdAt: string
  updatedAt: string
  items: OrderItemResponse[]
}

export interface PaymentResponse {
  id: string
  tenantId: string
  orderId: string
  amount: number
  paymentMethod: PaymentMethod
  amountReceived: number
  changeGiven: number
  status: string
  referenceNumber: string | null
  notes: string | null
  createdAt: string
  createdBy: string
}

// ─── Query Params ──────────────────────────────────────────────────────────────

export interface OrdersQueryParams {
  branchId?: string
  date?: string // YYYY-MM-DD
  status?: OrderStatus
  page?: number
  size?: number
}

export interface OrdersPageResponse {
  content: OrderResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
