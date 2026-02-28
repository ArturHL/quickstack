import { useMutation, useQueryClient } from '@tanstack/react-query'
import { customerApi } from '../api/customerApi'
import type { CustomerCreateRequest } from '../types/Customer'

export const useCreateCustomerMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CustomerCreateRequest) => customerApi.createCustomer(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}
