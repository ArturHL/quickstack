import { useQuery } from '@tanstack/react-query'
import { comboApi } from '../api/comboApi'

export const useCombosQuery = () => {
  return useQuery({
    queryKey: ['combos'],
    queryFn: () => comboApi.getCombos(),
    staleTime: 2 * 60 * 1000,
  })
}
