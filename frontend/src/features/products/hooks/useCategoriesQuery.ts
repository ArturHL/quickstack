import { useQuery } from '@tanstack/react-query'
import { categoryApi } from '../api/categoryApi'

export const useCategoriesQuery = () => {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => categoryApi.getCategories(),
    staleTime: 10 * 60 * 1000,
  })
}
