import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../api/orderApi'
import type { PaymentRequest } from '../types/Order'

export function useRegisterPaymentMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: PaymentRequest) => orderApi.registerPayment(request),
    onSuccess: (_data, request) => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['order', request.orderId] })
    },
  })
}
