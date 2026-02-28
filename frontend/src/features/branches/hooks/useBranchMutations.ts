import { useMutation, useQueryClient } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'
import type { BranchCreateRequest, BranchUpdateRequest } from '../types/Branch'

export const useCreateBranchMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: BranchCreateRequest) => branchApi.createBranch(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['branches'] }),
  })
}

export const useUpdateBranchMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: BranchUpdateRequest }) =>
      branchApi.updateBranch(id, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['branches'] }),
  })
}

export const useDeleteBranchMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => branchApi.deleteBranch(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['branches'] }),
  })
}
