import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../api/orderApi'
import type { OrderCreateRequest } from '../types/Order'

export function useCreateOrderMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: OrderCreateRequest) => orderApi.createOrder(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
  })
}
