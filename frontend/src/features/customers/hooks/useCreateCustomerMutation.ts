import { useMutation, useQueryClient } from '@tanstack/react-query'
import { customerAdminApi, type CustomerCreateRequest } from '../api/customerApi'

export const useCreateCustomerMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: CustomerCreateRequest) => customerAdminApi.createCustomer(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers-admin'] })
    },
  })
}
