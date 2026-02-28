import { useMutation, useQueryClient } from '@tanstack/react-query'
import { customerAdminApi, type CustomerUpdateRequest } from '../api/customerApi'

export const useUpdateCustomerMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CustomerUpdateRequest }) =>
      customerAdminApi.updateCustomer(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers-admin'] })
    },
  })
}
