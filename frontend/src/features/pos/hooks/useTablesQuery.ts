import { useQuery } from '@tanstack/react-query'
import { tableApi } from '../api/tableApi'

export const useTablesQuery = (areaId: string | null) => {
  return useQuery({
    queryKey: ['tables', areaId],
    queryFn: () => tableApi.getTablesByArea(areaId!),
    enabled: !!areaId,
    staleTime: 60 * 1000,
  })
}
