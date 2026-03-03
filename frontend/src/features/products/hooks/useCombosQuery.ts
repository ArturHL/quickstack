import { useQuery } from '@tanstack/react-query'
import { comboApi } from '../api/comboApi'

export const useCombosQuery = (params: { page?: number; size?: number } = {}) => {
  return useQuery({
    queryKey: ['combos', params],
    queryFn: () => comboApi.getCombos(params),
    staleTime: 2 * 60 * 1000,
  })
}
