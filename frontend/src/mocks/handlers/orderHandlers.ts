import { http, HttpResponse } from 'msw'
import type { OrderResponse, OrderStatus, PaymentResponse } from '../../features/pos/types/Order'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

// Status ID mapping (mirrors backend order_statuses table)
export const ORDER_STATUS_IDS: Record<OrderStatus, string> = {
  PENDING: 'd1111111-1111-1111-1111-111111111111',
  IN_PROGRESS: 'd2222222-2222-2222-2222-222222222222',
  READY: 'd3333333-3333-3333-3333-333333333333',
  DELIVERED: 'd4444444-4444-4444-4444-444444444444',
  COMPLETED: 'd5555555-5555-5555-5555-555555555555',
  CANCELLED: 'd6666666-6666-6666-6666-666666666666',
}

export const STATUS_ID_TO_NAME: Record<string, OrderStatus> = Object.fromEntries(
  Object.entries(ORDER_STATUS_IDS).map(([k, v]) => [v, k as OrderStatus])
) as Record<string, OrderStatus>

export const mockOrder: OrderResponse = {
  id: 'order-1',
  tenantId: 'tenant-1',
  branchId: 'branch-1',
  tableId: null,
  customerId: null,
  orderNumber: 'ORD-20260228-001',
  dailySequence: 1,
  serviceType: 'COUNTER',
  statusId: ORDER_STATUS_IDS.PENDING,
  subtotal: 100,
  taxRate: 0.16,
  tax: 16,
  discount: 0,
  total: 116,
  source: 'POS',
  notes: null,
  kitchenNotes: null,
  openedAt: '2026-02-28T10:00:00Z',
  closedAt: null,
  createdBy: 'user-1',
  updatedBy: 'user-1',
  createdAt: '2026-02-28T10:00:00Z',
  updatedAt: '2026-02-28T10:00:00Z',
  items: [
    {
      id: 'item-1',
      tenantId: 'tenant-1',
      orderId: 'order-1',
      productId: 'prod-1',
      variantId: null,
      comboId: null,
      productName: 'Hamburguesa Clásica',
      variantName: null,
      quantity: 2,
      unitPrice: 50,
      modifiersTotal: 0,
      lineTotal: 100,
      kdsStatus: 'PENDING',
      kdsSentAt: null,
      kdsReadyAt: null,
      notes: null,
      sortOrder: 1,
      modifiers: [],
      createdAt: '2026-02-28T10:00:00Z',
      updatedAt: '2026-02-28T10:00:00Z',
    },
  ],
}

export const mockOrderInProgress: OrderResponse = {
  ...mockOrder,
  id: 'order-2',
  orderNumber: 'ORD-20260228-002',
  dailySequence: 2,
  serviceType: 'DINE_IN',
  statusId: ORDER_STATUS_IDS.IN_PROGRESS,
  tableId: 'table-1',
  subtotal: 200,
  tax: 32,
  total: 232,
  openedAt: '2026-02-28T11:00:00Z',
  createdAt: '2026-02-28T11:00:00Z',
  updatedAt: '2026-02-28T11:00:00Z',
  items: [
    {
      ...mockOrder.items[0],
      id: 'item-2',
      orderId: 'order-2',
      productName: 'Pizza Pepperoni',
      quantity: 1,
      unitPrice: 200,
      lineTotal: 200,
    },
  ],
}

export const mockReadyOrder: OrderResponse = {
  ...mockOrder,
  id: 'order-ready',
  orderNumber: 'ORD-20260228-003',
  dailySequence: 3,
  serviceType: 'COUNTER',
  statusId: ORDER_STATUS_IDS.READY,
  subtotal: 89,
  tax: 14.24,
  total: 103.24,
  openedAt: '2026-02-28T12:00:00Z',
  createdAt: '2026-02-28T12:00:00Z',
  updatedAt: '2026-02-28T12:00:00Z',
  items: [
    {
      ...mockOrder.items[0],
      id: 'item-3',
      orderId: 'order-ready',
      productName: 'Café Americano',
      quantity: 1,
      unitPrice: 45,
      modifiersTotal: 10,
      lineTotal: 55,
      kdsStatus: 'READY',
      modifiers: [
        {
          id: 'mod-1',
          modifierId: 'modifier-1',
          modifierName: 'Leche Extra',
          priceAdjustment: 10,
          quantity: 1,
        },
      ],
    },
    {
      ...mockOrder.items[0],
      id: 'item-4',
      orderId: 'order-ready',
      productName: 'Pan Dulce',
      quantity: 2,
      unitPrice: 17,
      lineTotal: 34,
      kdsStatus: 'READY',
      sortOrder: 2,
    },
  ],
}

export const mockCompletedOrder: OrderResponse = {
  ...mockOrder,
  id: 'order-completed',
  orderNumber: 'ORD-20260228-004',
  dailySequence: 4,
  statusId: ORDER_STATUS_IDS.COMPLETED,
  closedAt: '2026-02-28T13:00:00Z',
  openedAt: '2026-02-28T12:30:00Z',
  createdAt: '2026-02-28T12:30:00Z',
  updatedAt: '2026-02-28T13:00:00Z',
}

export const mockCancelledOrder: OrderResponse = {
  ...mockOrder,
  id: 'order-cancelled',
  orderNumber: 'ORD-20260228-005',
  dailySequence: 5,
  statusId: ORDER_STATUS_IDS.CANCELLED,
  closedAt: '2026-02-28T14:00:00Z',
  openedAt: '2026-02-28T13:30:00Z',
  createdAt: '2026-02-28T13:30:00Z',
  updatedAt: '2026-02-28T14:00:00Z',
}

const allMockOrders: OrderResponse[] = [
  mockOrder,
  mockOrderInProgress,
  mockReadyOrder,
  mockCompletedOrder,
  mockCancelledOrder,
]

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
  // GET /orders — list with filters
  http.get(`${BASE}/orders`, ({ request }) => {
    const url = new URL(request.url)
    const statusParam = url.searchParams.get('status')
    let filtered = [...allMockOrders]

    if (statusParam) {
      const statusId = ORDER_STATUS_IDS[statusParam as OrderStatus]
      if (statusId) {
        filtered = filtered.filter((o) => o.statusId === statusId)
      }
    }

    return HttpResponse.json({
      data: {
        content: filtered,
        totalElements: filtered.length,
        totalPages: 1,
        number: 0,
        size: 20,
      },
    })
  }),

  // POST /orders — create
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

  // GET /orders/:orderId — single order
  http.get(`${BASE}/orders/:orderId`, ({ params }) => {
    const { orderId } = params
    const found = allMockOrders.find((o) => o.id === orderId)
    if (found) {
      return HttpResponse.json({ data: found })
    }
    return HttpResponse.json({ data: { ...mockOrder, id: orderId as string } })
  }),

  // POST /orders/:orderId/cancel
  http.post(`${BASE}/orders/:orderId/cancel`, ({ params }) => {
    const { orderId } = params
    return HttpResponse.json({
      data: { ...mockOrder, id: orderId as string, statusId: ORDER_STATUS_IDS.CANCELLED },
    })
  }),

  // POST /orders/:orderId/submit
  http.post(`${BASE}/orders/:orderId/submit`, ({ params }) => {
    const { orderId } = params
    return HttpResponse.json({
      data: { ...mockOrder, id: orderId as string, statusId: ORDER_STATUS_IDS.IN_PROGRESS },
    })
  }),

  // POST /orders/:orderId/ready
  http.post(`${BASE}/orders/:orderId/ready`, ({ params }) => {
    const { orderId } = params
    return HttpResponse.json({
      data: { ...mockOrder, id: orderId as string, statusId: ORDER_STATUS_IDS.READY },
    })
  }),

  // GET /orders/:orderId/payments
  http.get(`${BASE}/orders/:orderId/payments`, ({ params }) => {
    const { orderId } = params
    if (orderId === 'order-completed' || orderId === 'order-ready') {
      return HttpResponse.json({
        data: [{ ...mockPayment, orderId: orderId as string }],
      })
    }
    return HttpResponse.json({ data: [] })
  }),

  // POST /payments — register payment
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
