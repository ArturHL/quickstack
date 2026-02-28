import { describe, it, expect, beforeEach } from 'vitest'
import {
  useCartStore,
  initialCartState,
  selectSubtotal,
  selectTax,
  selectTotal,
  TAX_RATE,
} from '../cartStore'

beforeEach(() => {
  sessionStorage.clear()
  useCartStore.setState(initialCartState)
})

const baseItem = {
  productId: 'prod-1',
  productName: 'Café Americano',
  quantity: 1,
  unitPrice: 35.0,
  selectedModifiers: [],
}

describe('cartStore', () => {
  it('initial state has empty items and null service details', () => {
    const state = useCartStore.getState()

    expect(state.items).toHaveLength(0)
    expect(state.serviceType).toBeNull()
    expect(state.tableId).toBeNull()
    expect(state.customerId).toBeNull()
  })

  it('addItem adds item to cart', () => {
    useCartStore.getState().addItem(baseItem)

    const { items } = useCartStore.getState()
    expect(items).toHaveLength(1)
    expect(items[0].productName).toBe('Café Americano')
  })

  it('addItem computes lineTotal from unitPrice × quantity', () => {
    useCartStore.getState().addItem({ ...baseItem, quantity: 3, unitPrice: 35.0 })

    expect(useCartStore.getState().items[0].lineTotal).toBe(105.0)
  })

  it('addItem includes modifier price adjustments in lineTotal', () => {
    useCartStore.getState().addItem({
      ...baseItem,
      quantity: 2,
      unitPrice: 35.0,
      selectedModifiers: [{ modifierId: 'mod-1', modifierName: 'Leche extra', priceAdjustment: 5.0 }],
    })

    // (35 + 5) × 2 = 80
    expect(useCartStore.getState().items[0].lineTotal).toBe(80.0)
  })

  it('removeItem removes item by index', () => {
    useCartStore.getState().addItem(baseItem)
    useCartStore.getState().addItem({ ...baseItem, productId: 'prod-2', productName: 'Cappuccino' })

    useCartStore.getState().removeItem(0)

    const { items } = useCartStore.getState()
    expect(items).toHaveLength(1)
    expect(items[0].productName).toBe('Cappuccino')
  })

  it('updateQuantity updates quantity and recomputes lineTotal', () => {
    useCartStore.getState().addItem(baseItem) // lineTotal = 35

    useCartStore.getState().updateQuantity(0, 3)

    const item = useCartStore.getState().items[0]
    expect(item.quantity).toBe(3)
    expect(item.lineTotal).toBe(105.0)
  })

  it('updateQuantity ignores quantity <= 0', () => {
    useCartStore.getState().addItem(baseItem)

    useCartStore.getState().updateQuantity(0, 0)

    expect(useCartStore.getState().items[0].quantity).toBe(1)
  })

  it('clearCart resets all state', () => {
    useCartStore.getState().addItem(baseItem)
    useCartStore.getState().setServiceDetails('DINE_IN', 'table-1')

    useCartStore.getState().clearCart()

    const state = useCartStore.getState()
    expect(state.items).toHaveLength(0)
    expect(state.serviceType).toBeNull()
  })

  it('setServiceDetails sets serviceType, tableId and customerId', () => {
    useCartStore.getState().setServiceDetails('DINE_IN', 'table-1', 'cust-1')

    const state = useCartStore.getState()
    expect(state.serviceType).toBe('DINE_IN')
    expect(state.tableId).toBe('table-1')
    expect(state.customerId).toBe('cust-1')
  })

  it('selectSubtotal sums all lineTotals', () => {
    useCartStore.getState().addItem({ ...baseItem, quantity: 2, unitPrice: 35.0 }) // 70
    useCartStore.getState().addItem({ ...baseItem, productId: 'p2', quantity: 1, unitPrice: 50.0 }) // 50

    expect(selectSubtotal(useCartStore.getState())).toBe(120.0)
  })

  it('selectTax = subtotal × TAX_RATE, selectTotal = subtotal + tax', () => {
    useCartStore.getState().addItem({ ...baseItem, quantity: 1, unitPrice: 100.0 })

    const state = useCartStore.getState()
    const subtotal = selectSubtotal(state)
    const tax = selectTax(state)
    const total = selectTotal(state)

    expect(subtotal).toBe(100.0)
    expect(tax).toBe(Math.round(100 * TAX_RATE * 100) / 100)
    expect(total).toBe(subtotal + tax)
  })
})
