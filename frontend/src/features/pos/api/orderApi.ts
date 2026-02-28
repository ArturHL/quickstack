import axiosInstance from '../../../utils/axiosInstance'
import type { OrderCreateRequest, OrderResponse, OrdersQueryParams, OrdersPageResponse, PaymentRequest, PaymentResponse } from '../types/Order'

export const orderApi = {
  createOrder: (request: OrderCreateRequest): Promise<OrderResponse> =>
    axiosInstance
      .post<{ data: OrderResponse }>('/api/v1/orders', request)
      .then((r) => r.data.data),

  getOrders: (params?: OrdersQueryParams): Promise<OrdersPageResponse> =>
    axiosInstance
      .get<{ data: OrdersPageResponse }>('/api/v1/orders', { params })
      .then((r) => r.data.data),

  getOrder: (orderId: string): Promise<OrderResponse> =>
    axiosInstance
      .get<{ data: OrderResponse }>(`/api/v1/orders/${orderId}`)
      .then((r) => r.data.data),

  cancelOrder: (orderId: string): Promise<OrderResponse> =>
    axiosInstance
      .post<{ data: OrderResponse }>(`/api/v1/orders/${orderId}/cancel`)
      .then((r) => r.data.data),

  submitOrder: (orderId: string): Promise<OrderResponse> =>
    axiosInstance
      .post<{ data: OrderResponse }>(`/api/v1/orders/${orderId}/submit`)
      .then((r) => r.data.data),

  markOrderReady: (orderId: string): Promise<OrderResponse> =>
    axiosInstance
      .post<{ data: OrderResponse }>(`/api/v1/orders/${orderId}/ready`)
      .then((r) => r.data.data),

  registerPayment: (request: PaymentRequest): Promise<PaymentResponse> =>
    axiosInstance
      .post<{ data: PaymentResponse }>('/api/v1/payments', request)
      .then((r) => r.data.data),

  getPaymentsForOrder: (orderId: string): Promise<PaymentResponse[]> =>
    axiosInstance
      .get<{ data: PaymentResponse[] }>(`/api/v1/orders/${orderId}/payments`)
      .then((r) => r.data.data),
}
