import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../../pos/api/orderApi'

export const useCancelOrderMutation = () => {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => orderApi.cancelOrder(orderId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['orders'] })
            queryClient.invalidateQueries({ queryKey: ['order'] })
        },
    })
}
