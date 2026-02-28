import type { CartItem } from '../types/Cart'
import type { OrderCreateRequest, OrderItemRequest } from '../types/Order'

interface CartStateForOrder {
  items: CartItem[]
  serviceType: 'DINE_IN' | 'COUNTER' | 'DELIVERY' | 'TAKEOUT'
  tableId: string | null
  customerId: string | null
}

/**
 * Transforms the cartStore state into an OrderCreateRequest for the backend.
 * All prices are in their natural number form (the backend expects numbers).
 */
export function buildOrderRequest(cart: CartStateForOrder, branchId: string): OrderCreateRequest {
  const items: OrderItemRequest[] = cart.items.map((item: CartItem) => ({
    productId: item.comboId ? undefined : item.productId,
    variantId: item.variantId,
    comboId: item.comboId,
    productName: item.productName,
    variantName: item.variantName,
    quantity: item.quantity,
    unitPrice: item.unitPrice,
    modifiers: item.selectedModifiers.map((m) => ({
      modifierId: m.modifierId,
      modifierName: m.modifierName,
      priceAdjustment: m.priceAdjustment,
      quantity: 1,
    })),
  }))

  return {
    branchId,
    serviceType: cart.serviceType,
    tableId: cart.tableId ?? undefined,
    customerId: cart.customerId ?? undefined,
    items,
  }
}
