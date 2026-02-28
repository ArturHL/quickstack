import { useMutation, useQueryClient } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'
import type { AreaCreateRequest } from '../types/Branch'

export const useCreateAreaMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ branchId, body }: { branchId: string; body: AreaCreateRequest }) =>
      branchApi.createArea(branchId, body),
    onSuccess: (_data, { branchId }) =>
      queryClient.invalidateQueries({ queryKey: ['areas', branchId] }),
  })
}

export const useUpdateAreaMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { areaId: string; branchId: string; body: AreaCreateRequest }) =>
      branchApi.updateArea(vars.areaId, vars.body),
    onSuccess: (_data, { branchId }) =>
      queryClient.invalidateQueries({ queryKey: ['areas', branchId] }),
  })
}

export const useDeleteAreaMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { areaId: string; branchId: string }) =>
      branchApi.deleteArea(vars.areaId),
    onSuccess: (_data, { branchId }) =>
      queryClient.invalidateQueries({ queryKey: ['areas', branchId] }),
  })
}
