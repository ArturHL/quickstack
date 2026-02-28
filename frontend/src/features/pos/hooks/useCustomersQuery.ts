import { useQuery } from '@tanstack/react-query'
import { customerApi } from '../api/customerApi'

export const useCustomersQuery = (searchTerm: string) => {
  return useQuery({
    queryKey: ['customers', searchTerm],
    queryFn: () => customerApi.searchCustomers(searchTerm),
    enabled: searchTerm.length > 0,
    staleTime: 60 * 1000,
  })
}
