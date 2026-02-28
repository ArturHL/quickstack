import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'

interface BranchState {
  activeBranchId: string | null
}

interface BranchActions {
  setActiveBranchId: (id: string | null) => void
}

export const initialBranchState: BranchState = {
  activeBranchId: null,
}

export const useBranchStore = create<BranchState & BranchActions>()(
  persist(
    (set) => ({
      ...initialBranchState,
      setActiveBranchId: (id) => set({ activeBranchId: id }),
    }),
    {
      name: 'quickstack-branch',
      storage: createJSONStorage(() => localStorage),
    }
  )
)
