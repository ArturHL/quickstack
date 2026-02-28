import { http, HttpResponse } from 'msw'
import type { CustomerResponse, CustomerPage } from '../../features/pos/types/Customer'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockCustomers: CustomerResponse[] = [
  {
    id: 'cust-1',
    tenantId: 'tenant-1',
    name: 'Juan García',
    phone: '5551234567',
    email: null,
    whatsapp: null,
    addressLine1: 'Calle Reforma 100',
    addressLine2: null,
    city: 'CDMX',
    postalCode: '06600',
    deliveryNotes: null,
    totalOrders: 5,
    totalSpent: '450.00',
    lastOrderAt: '2024-01-15T10:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
  {
    id: 'cust-2',
    tenantId: 'tenant-1',
    name: 'María López',
    phone: null,
    email: 'maria@example.com',
    whatsapp: '5559876543',
    addressLine1: null,
    addressLine2: null,
    city: null,
    postalCode: null,
    deliveryNotes: null,
    totalOrders: 2,
    totalSpent: '180.00',
    lastOrderAt: null,
    createdAt: '2024-01-05T00:00:00Z',
    updatedAt: '2024-01-05T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

const mockPage = (content: CustomerResponse[]): CustomerPage => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: 0,
  size: 20,
})

export const customerHandlers = [
  http.get(`${BASE}/customers`, ({ request }) => {
    const url = new URL(request.url)
    const search = url.searchParams.get('search') ?? ''
    const filtered = mockCustomers.filter(
      (c) =>
        c.name?.toLowerCase().includes(search.toLowerCase()) ||
        c.phone?.includes(search) ||
        c.email?.toLowerCase().includes(search.toLowerCase())
    )
    return HttpResponse.json({ data: mockPage(filtered) }, { status: 200 })
  }),

  http.post(`${BASE}/customers`, async ({ request }) => {
    const body = (await request.json()) as Record<string, string>
    const newCustomer: CustomerResponse = {
      id: 'cust-new',
      tenantId: 'tenant-1',
      name: body.name ?? null,
      phone: body.phone ?? null,
      email: body.email ?? null,
      whatsapp: null,
      addressLine1: null,
      addressLine2: null,
      city: null,
      postalCode: null,
      deliveryNotes: null,
      totalOrders: 0,
      totalSpent: '0.00',
      lastOrderAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'cashier',
      updatedBy: 'cashier',
    }
    return HttpResponse.json({ data: newCustomer }, { status: 201 })
  }),
]
