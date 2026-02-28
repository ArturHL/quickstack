import { useQuery } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'

export const useBranchesQuery = () => {
  return useQuery({
    queryKey: ['branches'],
    queryFn: () => branchApi.getBranches(),
    staleTime: 5 * 60 * 1000,
  })
}
