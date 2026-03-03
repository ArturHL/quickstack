import { useMutation, useQueryClient } from '@tanstack/react-query'
import { modifierApi } from '../api/modifierApi'
import type { ModifierCreateRequest, ModifierUpdateRequest } from '../types/Product'

export const useCreateModifierMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ groupId, body }: { groupId: string; body: ModifierCreateRequest }) =>
      modifierApi.createModifier(groupId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}

export const useUpdateModifierMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ modifierId, body }: { modifierId: string; body: ModifierUpdateRequest }) =>
      modifierApi.updateModifier(modifierId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}

export const useDeleteModifierMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (modifierId: string) => modifierApi.deleteModifier(modifierId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}
