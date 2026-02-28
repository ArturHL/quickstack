import { useQuery } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'

export const useAreasQuery = (branchId: string | null) => {
  return useQuery({
    queryKey: ['areas', branchId],
    queryFn: () => branchApi.getAreasByBranch(branchId!),
    enabled: !!branchId,
    staleTime: 5 * 60 * 1000,
  })
}
