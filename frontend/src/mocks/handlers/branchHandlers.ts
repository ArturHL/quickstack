import { http, HttpResponse } from 'msw'
import type { BranchResponse } from '../../features/branches/types/Branch'
import type { AreaResponse, TableResponse } from '../../features/pos/types/Table'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockBranches: BranchResponse[] = [
  {
    id: 'branch-1',
    tenantId: 'tenant-1',
    name: 'Sucursal Centro',
    address: 'Av. Reforma 100',
    city: 'CDMX',
    phone: '5551234567',
    email: 'centro@quickstack.mx',
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'owner',
    updatedBy: 'owner',
  },
  {
    id: 'branch-2',
    tenantId: 'tenant-1',
    name: 'Sucursal Norte',
    address: 'Blvd. Insurgentes 200',
    city: 'CDMX',
    phone: null,
    email: null,
    isActive: true,
    createdAt: '2024-01-05T00:00:00Z',
    updatedAt: '2024-01-05T00:00:00Z',
    createdBy: 'owner',
    updatedBy: 'owner',
  },
]

const mockAreasDb: AreaResponse[] = [
  {
    id: 'area-1',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
    name: 'Terraza',
    description: null,
    sortOrder: 1,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

const mockTablesDb: TableResponse[] = [
  {
    id: 'table-1',
    tenantId: 'tenant-1',
    areaId: 'area-1',
    number: 1,
    name: null,
    capacity: 4,
    status: 'AVAILABLE',
    sortOrder: 1,
    positionX: null,
    positionY: null,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

export const branchHandlers = [
  http.get(`${BASE}/branches`, () => {
    return HttpResponse.json({ data: mockBranches }, { status: 200 })
  }),

  http.post(`${BASE}/branches`, async ({ request }) => {
    const body = (await request.json()) as Record<string, string>
    const newBranch: BranchResponse = {
      id: 'branch-new',
      tenantId: 'tenant-1',
      name: body.name ?? '',
      address: body.address ?? null,
      city: body.city ?? null,
      phone: body.phone ?? null,
      email: body.email ?? null,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'owner',
      updatedBy: 'owner',
    }
    return HttpResponse.json({ data: newBranch }, { status: 201 })
  }),

  http.put(`${BASE}/branches/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, string>
    const existing = mockBranches.find((b) => b.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: BranchResponse = { ...existing, name: body.name ?? existing.name }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/branches/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // Areas CRUD (branch context)
  http.post(`${BASE}/branches/:branchId/areas`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newArea: AreaResponse = {
      id: 'area-new',
      tenantId: 'tenant-1',
      branchId: params.branchId as string,
      name: (body.name as string) ?? '',
      description: (body.description as string) ?? null,
      sortOrder: 1,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'manager',
      updatedBy: 'manager',
    }
    return HttpResponse.json({ data: newArea }, { status: 201 })
  }),

  http.put(`${BASE}/areas/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockAreasDb.find((a) => a.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: AreaResponse = { ...existing, name: (body.name as string) ?? existing.name }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/areas/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  // Tables CRUD (area context)
  http.post(`${BASE}/areas/:areaId/tables`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const newTable: TableResponse = {
      id: 'table-new',
      tenantId: 'tenant-1',
      areaId: params.areaId as string,
      number: (body.number as number) ?? 1,
      name: (body.name as string) ?? null,
      capacity: (body.capacity as number) ?? 2,
      status: 'AVAILABLE',
      sortOrder: 1,
      positionX: null,
      positionY: null,
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: 'manager',
      updatedBy: 'manager',
    }
    return HttpResponse.json({ data: newTable }, { status: 201 })
  }),

  http.put(`${BASE}/tables/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, unknown>
    const existing = mockTablesDb.find((t) => t.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: TableResponse = {
      ...existing,
      capacity: (body.capacity as number) ?? existing.capacity,
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/tables/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]

export const branchErrorHandlers = {
  branchesServerError: () =>
    http.get(`${BASE}/branches`, () =>
      HttpResponse.json({ error: 'INTERNAL_ERROR' }, { status: 500 })
    ),
}
