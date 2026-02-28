import { useMutation, useQueryClient } from '@tanstack/react-query'
import { orderApi } from '../../pos/api/orderApi'

export const useMarkReadyMutation = () => {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (orderId: string) => orderApi.markOrderReady(orderId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['orders'] })
            queryClient.invalidateQueries({ queryKey: ['order'] })
        },
    })
}
