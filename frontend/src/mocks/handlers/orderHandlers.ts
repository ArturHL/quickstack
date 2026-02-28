import { http, HttpResponse } from 'msw'
import type { OrderResponse, PaymentResponse } from '../../features/pos/types/Order'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockOrder: OrderResponse = {
  id: 'order-1',
  tenantId: 'tenant-1',
  branchId: 'branch-1',
  tableId: null,
  customerId: null,
  orderNumber: 'ORD-20260228-001',
  dailySequence: 1,
  serviceType: 'COUNTER',
  statusId: 'd1111111-1111-1111-1111-111111111111',
  subtotal: 100,
  taxRate: 0.16,
  tax: 16,
  discount: 0,
  total: 116,
  source: 'POS',
  notes: null,
  kitchenNotes: null,
  openedAt: '2026-02-28T00:00:00Z',
  closedAt: null,
  createdBy: 'user-1',
  updatedBy: 'user-1',
  createdAt: '2026-02-28T00:00:00Z',
  updatedAt: '2026-02-28T00:00:00Z',
  items: [],
}

export const mockReadyOrder: OrderResponse = {
  ...mockOrder,
  id: 'order-ready',
  statusId: 'd3333333-3333-3333-3333-333333333333',
}

export const mockPayment: PaymentResponse = {
  id: 'payment-1',
  tenantId: 'tenant-1',
  orderId: 'order-ready',
  amount: 116,
  paymentMethod: 'CASH',
  amountReceived: 200,
  changeGiven: 84,
  status: 'COMPLETED',
  referenceNumber: null,
  notes: null,
  createdAt: '2026-02-28T00:00:00Z',
  createdBy: 'user-1',
}

export const orderHandlers = [
  http.post(`${BASE}/orders`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newOrder: OrderResponse = {
      ...mockOrder,
      id: 'order-new',
      serviceType: (body.serviceType as OrderResponse['serviceType']) ?? 'COUNTER',
      tableId: (body.tableId as string) ?? null,
      customerId: (body.customerId as string) ?? null,
    }
    return HttpResponse.json({ data: newOrder }, { status: 201 })
  }),

  http.get(`${BASE}/orders/:orderId`, ({ params }) => {
    const { orderId } = params
    if (orderId === 'order-ready') {
      return HttpResponse.json({ data: mockReadyOrder }, { status: 200 })
    }
    return HttpResponse.json({ data: { ...mockOrder, id: orderId as string } }, { status: 200 })
  }),

  http.post(`${BASE}/orders/:orderId/submit`, ({ params }) => {
    const { orderId } = params
    return HttpResponse.json(
      { data: { ...mockOrder, id: orderId as string, statusId: 'd2222222-2222-2222-2222-222222222222' } },
      { status: 200 }
    )
  }),

  http.post(`${BASE}/orders/:orderId/ready`, ({ params }) => {
    const { orderId } = params
    return HttpResponse.json(
      { data: { ...mockOrder, id: orderId as string, statusId: 'd3333333-3333-3333-3333-333333333333' } },
      { status: 200 }
    )
  }),

  http.post(`${BASE}/payments`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const payment: PaymentResponse = {
      ...mockPayment,
      orderId: (body.orderId as string) ?? 'order-1',
      amount: (body.amount as number) ?? 116,
      amountReceived: (body.amount as number) ?? 116,
      changeGiven: 0,
    }
    return HttpResponse.json({ data: payment }, { status: 201 })
  }),
]
