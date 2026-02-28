import { http, HttpResponse } from 'msw'
import type { ProductResponse, ProductPage, CategoryResponse } from '../../features/products/types/Product'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockCategories: CategoryResponse[] = [
  {
    id: 'cat-1',
    tenantId: 'tenant-1',
    name: 'Bebidas',
    description: null,
    imageUrl: null,
    sortOrder: 1,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
  {
    id: 'cat-2',
    tenantId: 'tenant-1',
    name: 'Comida',
    description: null,
    imageUrl: null,
    sortOrder: 2,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

export const mockProducts: ProductResponse[] = [
  {
    id: 'prod-1',
    tenantId: 'tenant-1',
    categoryId: 'cat-1',
    categoryName: 'Bebidas',
    name: 'Café Americano',
    description: null,
    sku: 'CAF-001',
    basePrice: 35,
    costPrice: 10,
    productType: 'SIMPLE',
    imageUrl: null,
    sortOrder: 1,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
  {
    id: 'prod-2',
    tenantId: 'tenant-1',
    categoryId: 'cat-2',
    categoryName: 'Comida',
    name: 'Sandwich Club',
    description: null,
    sku: 'SAN-001',
    basePrice: 80,
    costPrice: 30,
    productType: 'SIMPLE',
    imageUrl: null,
    sortOrder: 2,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

const mockPage = (content: ProductResponse[], page = 0, size = 20): ProductPage => ({
  content,
  totalElements: content.length,
  totalPages: 1,
  number: page,
  size,
})

export const productHandlers = [
  http.get(`${BASE}/categories`, () => {
    return HttpResponse.json({ data: mockCategories }, { status: 200 })
  }),

  http.get(`${BASE}/products`, ({ request }) => {
    const url = new URL(request.url)
    const search = url.searchParams.get('search') ?? ''
    const categoryId = url.searchParams.get('categoryId') ?? ''
    const page = parseInt(url.searchParams.get('page') ?? '0')
    const size = parseInt(url.searchParams.get('size') ?? '20')

    let filtered = [...mockProducts]
    if (search) {
      filtered = filtered.filter((p) => p.name.toLowerCase().includes(search.toLowerCase()))
    }
    if (categoryId) {
      filtered = filtered.filter((p) => p.categoryId === categoryId)
    }
    return HttpResponse.json({ data: mockPage(filtered, page, size) }, { status: 200 })
  }),

  http.get(`${BASE}/products/:id`, ({ params }) => {
    const product = mockProducts.find((p) => p.id === params.id)
    if (!product) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    return HttpResponse.json({ data: product }, { status: 200 })
  }),

  http.post(`${BASE}/products`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newProduct: ProductResponse = {
      id: 'prod-new',
      tenantId: 'tenant-1',
      categoryId: (body.categoryId as string) ?? null,
      categoryName: null,
      name: (body.name as string) ?? '',
      description: (body.description as string) ?? null,
      sku: (body.sku as string) ?? null,
      basePrice: (body.basePrice as number) ?? 0,
      costPrice: (body.costPrice as number) ?? null,
      productType: (body.productType as 'SIMPLE') ?? 'SIMPLE',
      imageUrl: null,
      sortOrder: 1,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'manager',
      updatedBy: 'manager',
    }
    return HttpResponse.json({ data: newProduct }, { status: 201 })
  }),

  http.put(`${BASE}/products/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockProducts.find((p) => p.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: ProductResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      basePrice: (body.basePrice as number) ?? existing.basePrice,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/products/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]

export const productErrorHandlers = {
  createConflict: () =>
    http.post(`${BASE}/products`, () =>
      HttpResponse.json({ error: 'DUPLICATE_SKU' }, { status: 409 })
    ),
}
