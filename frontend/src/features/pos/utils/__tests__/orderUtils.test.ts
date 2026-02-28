import { describe, it, expect } from 'vitest'
import { buildOrderRequest } from '../orderUtils'
import type { CartItem } from '../../types/Cart'

const branchId = 'branch-1'

const simpleItem: CartItem = {
  productId: 'prod-1',
  productName: 'Café Americano',
  quantity: 2,
  unitPrice: 45,
  selectedModifiers: [],
  lineTotal: 90,
}

const itemWithModifiers: CartItem = {
  productId: 'prod-2',
  productName: 'Tacos',
  quantity: 1,
  unitPrice: 80,
  selectedModifiers: [
    { modifierId: 'mod-1', modifierName: 'Extra salsa', priceAdjustment: 10 },
  ],
  lineTotal: 90,
}

const itemWithVariant: CartItem = {
  productId: 'prod-3',
  variantId: 'var-1',
  productName: 'Refresco',
  variantName: 'Grande',
  quantity: 1,
  unitPrice: 35,
  selectedModifiers: [],
  lineTotal: 35,
}

describe('buildOrderRequest', () => {
  it('maps COUNTER cart to correct request structure', () => {
    const cart = { items: [simpleItem], serviceType: 'COUNTER' as const, tableId: null, customerId: null }
    const request = buildOrderRequest(cart, branchId)

    expect(request.branchId).toBe(branchId)
    expect(request.serviceType).toBe('COUNTER')
    expect(request.tableId).toBeUndefined()
    expect(request.customerId).toBeUndefined()
    expect(request.items).toHaveLength(1)
    expect(request.items[0].productName).toBe('Café Americano')
    expect(request.items[0].quantity).toBe(2)
    expect(request.items[0].unitPrice).toBe(45)
  })

  it('maps DINE_IN cart with tableId', () => {
    const cart = { items: [simpleItem], serviceType: 'DINE_IN' as const, tableId: 'table-3', customerId: null }
    const request = buildOrderRequest(cart, branchId)

    expect(request.serviceType).toBe('DINE_IN')
    expect(request.tableId).toBe('table-3')
    expect(request.customerId).toBeUndefined()
  })

  it('maps DELIVERY cart with customerId', () => {
    const cart = { items: [simpleItem], serviceType: 'DELIVERY' as const, tableId: null, customerId: 'cust-1' }
    const request = buildOrderRequest(cart, branchId)

    expect(request.serviceType).toBe('DELIVERY')
    expect(request.customerId).toBe('cust-1')
    expect(request.tableId).toBeUndefined()
  })

  it('maps modifiers correctly into OrderItemModifierRequest', () => {
    const cart = { items: [itemWithModifiers], serviceType: 'COUNTER' as const, tableId: null, customerId: null }
    const request = buildOrderRequest(cart, branchId)

    const itemReq = request.items[0]
    expect(itemReq.modifiers).toHaveLength(1)
    expect(itemReq.modifiers[0].modifierId).toBe('mod-1')
    expect(itemReq.modifiers[0].modifierName).toBe('Extra salsa')
    expect(itemReq.modifiers[0].priceAdjustment).toBe(10)
    expect(itemReq.modifiers[0].quantity).toBe(1)
  })

  it('maps item with variant correctly', () => {
    const cart = { items: [itemWithVariant], serviceType: 'COUNTER' as const, tableId: null, customerId: null }
    const request = buildOrderRequest(cart, branchId)

    const itemReq = request.items[0]
    expect(itemReq.productId).toBe('prod-3')
    expect(itemReq.variantId).toBe('var-1')
    expect(itemReq.variantName).toBe('Grande')
    expect(itemReq.modifiers).toHaveLength(0)
  })

  it('maps multiple items preserving order', () => {
    const cart = {
      items: [simpleItem, itemWithModifiers, itemWithVariant],
      serviceType: 'COUNTER' as const,
      tableId: null,
      customerId: null,
    }
    const request = buildOrderRequest(cart, branchId)

    expect(request.items).toHaveLength(3)
    expect(request.items[0].productName).toBe('Café Americano')
    expect(request.items[1].productName).toBe('Tacos')
    expect(request.items[2].productName).toBe('Refresco')
  })
})
