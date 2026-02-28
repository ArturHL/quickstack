import { useQuery } from '@tanstack/react-query'
import { orderApi } from '../../pos/api/orderApi'
import type { OrdersQueryParams } from '../../pos/types/Order'

export const useOrdersQuery = (params?: OrdersQueryParams) =>
    useQuery({
        queryKey: ['orders', params],
        queryFn: () => orderApi.getOrders(params),
        refetchInterval: 30_000,
    })
