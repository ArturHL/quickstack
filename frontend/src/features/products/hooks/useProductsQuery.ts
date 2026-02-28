import { useQuery } from '@tanstack/react-query'
import { productApi, type ProductListParams } from '../api/productApi'

export const useProductsQuery = (params: ProductListParams = {}) => {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => productApi.getProducts(params),
    staleTime: 2 * 60 * 1000,
  })
}
