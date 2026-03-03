import { useMutation, useQueryClient } from '@tanstack/react-query'
import { comboApi } from '../api/comboApi'
import type { ComboCreateRequest, ComboUpdateRequest } from '../types/Product'

export const useCreateComboMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ComboCreateRequest) => comboApi.createCombo(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['combos'] })
    },
  })
}

export const useUpdateComboMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: ComboUpdateRequest }) =>
      comboApi.updateCombo(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['combos'] })
    },
  })
}

export const useDeleteComboMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => comboApi.deleteCombo(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['combos'] })
    },
  })
}
