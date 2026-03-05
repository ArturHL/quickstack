import { useQuery } from '@tanstack/react-query'
import { customerAdminApi } from '../api/customerApi'

export const useCustomerQuery = (customerId: string | null | undefined) => {
  return useQuery({
    queryKey: ['customer', customerId],
    queryFn: () => customerAdminApi.getCustomer(customerId!),
    enabled: !!customerId,
    staleTime: 5 * 60 * 1000,
  })
}
