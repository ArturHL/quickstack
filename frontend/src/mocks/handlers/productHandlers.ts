import { http, HttpResponse } from 'msw'
import type { ProductResponse, ProductPage, CategoryResponse, VariantResponse } from '../../features/products/types/Product'

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
    category: { id: 'cat-1', name: 'Bebidas', sortOrder: 1, isActive: true, parentId: null },
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
    category: { id: 'cat-2', name: 'Comida', sortOrder: 2, isActive: true, parentId: null },
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
  {
    id: 'prod-3',
    tenantId: 'tenant-1',
    category: { id: 'cat-1', name: 'Bebidas', sortOrder: 1, isActive: true, parentId: null },
    name: 'Café con Leche',
    description: null,
    sku: 'CAF-002',
    basePrice: 0,
    costPrice: null,
    productType: 'VARIANT',
    imageUrl: null,
    sortOrder: 3,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

export const mockVariants: VariantResponse[] = [
  {
    id: 'var-1',
    productId: 'prod-3',
    tenantId: 'tenant-1',
    name: 'Chico',
    priceAdjustment: 0,
    effectivePrice: 35,
    isDefault: true,
    sortOrder: 1,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 'var-2',
    productId: 'prod-3',
    tenantId: 'tenant-1',
    name: 'Grande',
    priceAdjustment: 10,
    effectivePrice: 45,
    isDefault: false,
    sortOrder: 2,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
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
    return HttpResponse.json({
      data: { content: mockCategories, totalElements: mockCategories.length, totalPages: 1, number: 0, size: 20 },
    }, { status: 200 })
  }),

  http.post(`${BASE}/categories`, async ({ request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newCategory: CategoryResponse = {
      id: 'cat-new',
      tenantId: 'tenant-1',
      name: (body.name as string) ?? '',
      description: (body.description as string) ?? null,
      imageUrl: null,
      sortOrder: 99,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'manager',
      updatedBy: 'manager',
    }
    return HttpResponse.json({ data: newCategory }, { status: 201 })
  }),

  http.put(`${BASE}/categories/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockCategories.find((c) => c.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: CategoryResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      description: (body.description as string) ?? existing.description,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/categories/:id`, () => {
    return new HttpResponse(null, { status: 204 })
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
      filtered = filtered.filter((p) => p.category?.id === categoryId)
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
      category: null,
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

  // Variant handlers
  http.get(`${BASE}/products/:productId/variants`, ({ params }) => {
    const variants = mockVariants.filter((v) => v.productId === params.productId)
    return HttpResponse.json({ data: variants }, { status: 200 })
  }),

  http.post(`${BASE}/products/:productId/variants`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newVariant: VariantResponse = {
      id: 'var-new',
      productId: params.productId as string,
      tenantId: 'tenant-1',
      name: (body.name as string) ?? '',
      priceAdjustment: (body.priceAdjustment as number) ?? 0,
      effectivePrice: (body.priceAdjustment as number) ?? 0,
      isDefault: false,
      sortOrder: 99,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: newVariant }, { status: 201 })
  }),

  http.put(`${BASE}/products/:productId/variants/:variantId`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockVariants.find((v) => v.id === params.variantId)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: VariantResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      priceAdjustment: (body.priceAdjustment as number) ?? existing.priceAdjustment,
      effectivePrice: (body.priceAdjustment as number) ?? existing.effectivePrice,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/products/:productId/variants/:variantId`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]

export const productErrorHandlers = {
  createConflict: () =>
    http.post(`${BASE}/products`, () =>
      HttpResponse.json({ error: 'DUPLICATE_SKU' }, { status: 409 })
    ),
}
