import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../api/orderApi'

export function useMarkReadyMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (orderId: string) => orderApi.markOrderReady(orderId),
    onSuccess: (_data, orderId) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['order', orderId] })
    },
  })
}
