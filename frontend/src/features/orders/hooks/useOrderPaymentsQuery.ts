import { useQuery } from '@tanstack/react-query'
import { orderApi } from '../../pos/api/orderApi'

export const useOrderPaymentsQuery = (orderId: string) =>
    useQuery({
        queryKey: ['order-payments', orderId],
        queryFn: () => orderApi.getPaymentsForOrder(orderId),
        enabled: !!orderId,
    })
