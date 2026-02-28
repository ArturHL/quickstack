import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../api/orderApi'

export function useSubmitOrderMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (orderId: string) => orderApi.submitOrder(orderId),
    onSuccess: (_data, orderId) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['order', orderId] })
    },
  })
}
