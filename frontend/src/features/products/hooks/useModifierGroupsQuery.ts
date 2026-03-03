import { useQuery } from '@tanstack/react-query'
import { modifierApi } from '../api/modifierApi'

export const useModifierGroupsQuery = (productId: string) => {
  return useQuery({
    queryKey: ['modifier-groups', productId],
    queryFn: () => modifierApi.getModifierGroups(productId),
    staleTime: 5 * 60 * 1000,
  })
}
