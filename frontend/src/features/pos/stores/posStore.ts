import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

interface PosState {
  currentOrderId: string | null
}

interface PosActions {
  setCurrentOrderId: (id: string | null) => void
  clearCurrentOrder: () => void
}

export const initialPosState: PosState = {
  currentOrderId: null,
}

export const usePosStore = create<PosState & PosActions>()(
  persist(
    (set) => ({
      ...initialPosState,
      setCurrentOrderId: (id) => set({ currentOrderId: id }),
      clearCurrentOrder: () => set({ currentOrderId: null }),
    }),
    {
      name: 'quickstack-pos',
      storage: createJSONStorage(() => sessionStorage),
    }
  )
)
