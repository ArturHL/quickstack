import { http, HttpResponse } from 'msw'
import type { ComboResponse, ComboPage } from '../../features/products/types/Product'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockCombos: ComboResponse[] = [
  {
    id: 'combo-1',
    tenantId: 'tenant-1',
    name: 'Combo Desayuno',
    description: 'Café + Sandwich',
    imageUrl: null,
    price: 105,
    sortOrder: 1,
    isActive: true,
    items: [
      { productId: 'prod-1', productName: 'Café Americano', quantity: 1 },
      { productId: 'prod-2', productName: 'Sandwich Club', quantity: 1 },
    ],
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 'combo-2',
    tenantId: 'tenant-1',
    name: 'Combo Doble Café',
    description: null,
    imageUrl: null,
    price: 60,
    sortOrder: 2,
    isActive: true,
    items: [
      { productId: 'prod-1', productName: 'Café Americano', quantity: 2 },
    ],
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
]

const mockPage = (content: ComboResponse[], page = 0, size = 20): ComboPage => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: page,
  size,
})

export const comboHandlers = [
  http.get(`${BASE}/combos`, ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') ?? '0')
    const size = parseInt(url.searchParams.get('size') ?? '20')
    return HttpResponse.json({ data: mockPage(mockCombos, page, size) }, { status: 200 })
  }),

  http.get(`${BASE}/combos/:id`, ({ params }) => {
    const combo = mockCombos.find((c) => c.id === params.id)
    if (!combo) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    return HttpResponse.json({ data: combo }, { status: 200 })
  }),

  http.post(`${BASE}/combos`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const items = (body.items as Array<{ productId: string; quantity: number }>) ?? []
    const newCombo: ComboResponse = {
      id: 'combo-new',
      tenantId: 'tenant-1',
      name: (body.name as string) ?? '',
      description: (body.description as string) ?? null,
      imageUrl: null,
      price: (body.price as number) ?? 0,
      sortOrder: 99,
      isActive: true,
      items: items.map((item) => ({
        productId: item.productId,
        productName: 'Producto',
        quantity: item.quantity,
      })),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: newCombo }, { status: 201 })
  }),

  http.put(`${BASE}/combos/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockCombos.find((c) => c.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: ComboResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      price: (body.price as number) ?? existing.price,
      description: body.description !== undefined ? (body.description as string | null) : existing.description,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/combos/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]
