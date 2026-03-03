import { useMutation, useQueryClient } from '@tanstack/react-query'
import { modifierApi } from '../api/modifierApi'
import type { ModifierGroupCreateRequest, ModifierGroupUpdateRequest } from '../types/Product'

export const useCreateModifierGroupMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ModifierGroupCreateRequest) =>
      modifierApi.createModifierGroup(productId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}

export const useUpdateModifierGroupMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ groupId, body }: { groupId: string; body: ModifierGroupUpdateRequest }) =>
      modifierApi.updateModifierGroup(groupId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}

export const useDeleteModifierGroupMutation = (productId: string) => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (groupId: string) => modifierApi.deleteModifierGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['modifier-groups', productId] })
    },
  })
}
