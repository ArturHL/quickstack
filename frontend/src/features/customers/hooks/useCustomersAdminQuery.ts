import { useQuery } from '@tanstack/react-query'
import { customerAdminApi } from '../api/customerApi'

export const useCustomersAdminQuery = (params: { search?: string; page?: number; size?: number } = {}) => {
  return useQuery({
    queryKey: ['customers-admin', params],
    queryFn: () => customerAdminApi.getCustomers(params),
    staleTime: 2 * 60 * 1000,
  })
}
