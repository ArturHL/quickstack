import { useQuery } from '@tanstack/react-query'
import { tableApi } from '../api/tableApi'

export const useAreasQuery = (branchId: string | null) => {
  return useQuery({
    queryKey: ['areas', branchId],
    queryFn: () => tableApi.getAreasByBranch(branchId!),
    enabled: !!branchId,
    staleTime: 5 * 60 * 1000,
  })
}
