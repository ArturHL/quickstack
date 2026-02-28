import { useMutation, useQueryClient } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'
import type { TableCreateRequest } from '../types/Branch'

export const useCreateTableMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ areaId, body }: { areaId: string; body: TableCreateRequest }) =>
      branchApi.createTable(areaId, body),
    onSuccess: (_data, { areaId }) =>
      queryClient.invalidateQueries({ queryKey: ['tables-admin', areaId] }),
  })
}

export const useUpdateTableMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { tableId: string; areaId: string; body: TableCreateRequest }) =>
      branchApi.updateTable(vars.tableId, vars.body),
    onSuccess: (_data, { areaId }) =>
      queryClient.invalidateQueries({ queryKey: ['tables-admin', areaId] }),
  })
}

export const useDeleteTableMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { tableId: string; areaId: string }) =>
      branchApi.deleteTable(vars.tableId),
    onSuccess: (_data, { areaId }) =>
      queryClient.invalidateQueries({ queryKey: ['tables-admin', areaId] }),
  })
}
