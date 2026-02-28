import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { CartItem, SelectedModifier, ServiceType } from '../types/Cart'

// IVA México — should come from tenant config in a future phase
export const TAX_RATE = 0.16

type AddItemInput = Omit<CartItem, 'lineTotal'>

function computeLineTotal(unitPrice: number, modifiers: SelectedModifier[], quantity: number): number {
  const modTotal = modifiers.reduce((sum, m) => sum + m.priceAdjustment, 0)
  return (unitPrice + modTotal) * quantity
}

interface CartState {
  items: CartItem[]
  serviceType: ServiceType | null
  tableId: string | null
  customerId: string | null
}

interface CartActions {
  addItem: (input: AddItemInput) => void
  removeItem: (index: number) => void
  updateQuantity: (index: number, quantity: number) => void
  clearCart: () => void
  setServiceDetails: (type: ServiceType, tableId?: string, customerId?: string) => void
}

export const initialCartState: CartState = {
  items: [],
  serviceType: null,
  tableId: null,
  customerId: null,
}

export const useCartStore = create<CartState & CartActions>()(
  persist(
    (set) => ({
      ...initialCartState,

      addItem: (input) => {
        const lineTotal = computeLineTotal(input.unitPrice, input.selectedModifiers, input.quantity)
        set((state) => ({ items: [...state.items, { ...input, lineTotal }] }))
      },

      removeItem: (index) => {
        set((state) => ({ items: state.items.filter((_, i) => i !== index) }))
      },

      updateQuantity: (index, quantity) => {
        if (quantity <= 0) return
        set((state) => ({
          items: state.items.map((item, i) => {
            if (i !== index) return item
            return { ...item, quantity, lineTotal: computeLineTotal(item.unitPrice, item.selectedModifiers, quantity) }
          }),
        }))
      },

      clearCart: () => set(initialCartState),

      setServiceDetails: (type, tableId, customerId) => {
        set({ serviceType: type, tableId: tableId ?? null, customerId: customerId ?? null })
      },
    }),
    {
      name: 'quickstack-cart',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
)

// ─── Selectors ────────────────────────────────────────────────────────────────

export const selectSubtotal = (state: CartState): number =>
  state.items.reduce((sum, item) => sum + item.lineTotal, 0)

export const selectTax = (state: CartState): number =>
  Math.round(selectSubtotal(state) * TAX_RATE * 100) / 100

export const selectTotal = (state: CartState): number =>
  selectSubtotal(state) + selectTax(state)
