import axiosInstance from '../../../utils/axiosInstance'
import type { OrderCreateRequest, OrderResponse, PaymentRequest, PaymentResponse } from '../types/Order'

export const orderApi = {
  createOrder: (request: OrderCreateRequest): Promise<OrderResponse> =>
    axiosInstance
      .post<{ data: OrderResponse }>('/api/v1/orders', request)
      .then((r) => r.data.data),

  getOrder: (orderId: string): Promise<OrderResponse> =>
    axiosInstance
      .get<{ data: OrderResponse }>(`/api/v1/orders/${orderId}`)
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
}
