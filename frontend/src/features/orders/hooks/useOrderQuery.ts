import { useQuery } from '@tanstack/react-query'
import { orderApi } from '../../pos/api/orderApi'

export const useOrderQuery = (orderId: string) =>
    useQuery({
        queryKey: ['order', orderId],
        queryFn: () => orderApi.getOrder(orderId),
        enabled: !!orderId,
    })
