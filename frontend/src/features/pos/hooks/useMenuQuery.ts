import { useQuery } from '@tanstack/react-query'
import { menuApi } from '../api/menuApi'

export const useMenuQuery = () => {
  return useQuery({
    queryKey: ['menu'],
    queryFn: () => menuApi.getMenu(),
    staleTime: 5 * 60 * 1000,
    retry: 2,
  })
}
