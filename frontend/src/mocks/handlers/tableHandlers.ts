import { http, HttpResponse } from 'msw'
import type { AreaResponse, TableResponse } from '../../features/pos/types/Table'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockAreas: AreaResponse[] = [
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
  {
    id: 'area-2',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
    name: 'Interior',
    description: null,
    sortOrder: 2,
    isActive: true,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    createdBy: 'admin',
    updatedBy: 'admin',
  },
]

export const mockTablesByArea: Record<string, TableResponse[]> = {
  'area-1': [
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
    {
      id: 'table-2',
      tenantId: 'tenant-1',
      areaId: 'area-1',
      number: 2,
      name: null,
      capacity: 2,
      status: 'OCCUPIED',
      sortOrder: 2,
      positionX: null,
      positionY: null,
      isActive: true,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
    },
  ],
  'area-2': [
    {
      id: 'table-3',
      tenantId: 'tenant-1',
      areaId: 'area-2',
      number: 3,
      name: 'VIP',
      capacity: 6,
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
  ],
}

export const tableHandlers = [
  http.get(`${BASE}/branches/:branchId/areas`, ({ params }) => {
    const areas = mockAreas.filter((a) => a.branchId === params.branchId)
    return HttpResponse.json({ data: areas }, { status: 200 })
  }),

  http.get(`${BASE}/areas/:areaId/tables`, ({ params }) => {
    const tables = mockTablesByArea[params.areaId as string] ?? []
    return HttpResponse.json({ data: tables }, { status: 200 })
  }),
]

export const tableErrorHandlers = {
  areasServerError: () =>
    http.get(`${BASE}/branches/:branchId/areas`, () =>
      HttpResponse.json({ error: 'INTERNAL_ERROR' }, { status: 500 })
    ),
}
