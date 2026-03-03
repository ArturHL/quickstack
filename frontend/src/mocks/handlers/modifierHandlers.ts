import { http, HttpResponse } from 'msw'
import type { ModifierGroupResponse, ModifierResponse } from '../../features/products/types/Product'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockModifiers: ModifierResponse[] = [
  {
    id: 'mod-1',
    groupId: 'grp-1',
    tenantId: 'tenant-1',
    name: 'Caliente',
    priceAdjustment: 0,
    isDefault: true,
    sortOrder: 1,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 'mod-2',
    groupId: 'grp-1',
    tenantId: 'tenant-1',
    name: 'Frío',
    priceAdjustment: 0,
    isDefault: false,
    sortOrder: 2,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
]

export const mockModifierGroups: ModifierGroupResponse[] = [
  {
    id: 'grp-1',
    productId: 'prod-1',
    tenantId: 'tenant-1',
    name: 'Temperatura',
    minSelections: 1,
    maxSelections: 1,
    isRequired: true,
    sortOrder: 1,
    isActive: true,
    modifiers: mockModifiers,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  },
]

export const modifierHandlers = [
  http.get(`${BASE}/products/:productId/modifier-groups`, ({ params }) => {
    const groups = mockModifierGroups.filter((g) => g.productId === params.productId)
    return HttpResponse.json({ data: groups }, { status: 200 })
  }),

  http.post(`${BASE}/products/:productId/modifier-groups`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newGroup: ModifierGroupResponse = {
      id: 'grp-new',
      productId: params.productId as string,
      tenantId: 'tenant-1',
      name: (body.name as string) ?? '',
      minSelections: (body.minSelections as number) ?? 0,
      maxSelections: (body.maxSelections as number | null) ?? null,
      isRequired: (body.isRequired as boolean) ?? false,
      sortOrder: 99,
      isActive: true,
      modifiers: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: newGroup }, { status: 201 })
  }),

  http.put(`${BASE}/modifier-groups/:groupId`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockModifierGroups.find((g) => g.id === params.groupId)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: ModifierGroupResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      minSelections: (body.minSelections as number) ?? existing.minSelections,
      maxSelections: body.maxSelections !== undefined
        ? (body.maxSelections as number | null)
        : existing.maxSelections,
      isRequired: body.isRequired !== undefined
        ? (body.isRequired as boolean)
        : existing.isRequired,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/modifier-groups/:groupId`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${BASE}/modifier-groups/:groupId/modifiers`, ({ params }) => {
    const modifiers = mockModifiers.filter((m) => m.groupId === params.groupId)
    return HttpResponse.json({ data: modifiers }, { status: 200 })
  }),

  http.post(`${BASE}/modifier-groups/:groupId/modifiers`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newModifier: ModifierResponse = {
      id: 'mod-new',
      groupId: params.groupId as string,
      tenantId: 'tenant-1',
      name: (body.name as string) ?? '',
      priceAdjustment: (body.priceAdjustment as number) ?? 0,
      isDefault: false,
      sortOrder: 99,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: newModifier }, { status: 201 })
  }),

  http.put(`${BASE}/modifiers/:modifierId`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockModifiers.find((m) => m.id === params.modifierId)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: ModifierResponse = {
      ...existing,
      name: (body.name as string) ?? existing.name,
      priceAdjustment: (body.priceAdjustment as number) ?? existing.priceAdjustment,
      updatedAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/modifiers/:modifierId`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]
