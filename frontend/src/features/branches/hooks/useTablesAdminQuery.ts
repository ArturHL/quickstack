import { useQuery } from '@tanstack/react-query'
import { branchApi } from '../api/branchApi'

export const useTablesAdminQuery = (areaId: string | null) => {
  return useQuery({
    queryKey: ['tables-admin', areaId],
    queryFn: () => branchApi.getTablesByArea(areaId!),
    enabled: !!areaId,
    staleTime: 5 * 60 * 1000,
  })
}
