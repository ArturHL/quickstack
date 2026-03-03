import { useQuery } from '@tanstack/react-query'
import { variantApi } from '../api/variantApi'

export const useVariantsQuery = (productId: string | undefined, enabled: boolean) => {
  return useQuery({
    queryKey: ['variants', productId],
    queryFn: () => variantApi.getVariants(productId!),
    enabled: !!productId && enabled,
    staleTime: 5 * 60 * 1000,
  })
}
