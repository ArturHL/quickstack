export type ServiceType = 'DINE_IN' | 'COUNTER' | 'DELIVERY' | 'TAKEOUT'

export interface SelectedModifier {
  modifierId: string
  modifierName: string
  priceAdjustment: number
}

export interface CartItem {
  productId: string
  variantId?: string
  comboId?: string
  productName: string
  variantName?: string
  quantity: number
  unitPrice: number
  selectedModifiers: SelectedModifier[]
  lineTotal: number
}
