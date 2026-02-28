import { useQuery } from '@tanstack/react-query'
import { productApi } from '../api/productApi'

export const useCategoriesQuery = () => {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => productApi.getCategories(),
    staleTime: 10 * 60 * 1000,
  })
}
