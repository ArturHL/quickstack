import { http, HttpResponse } from 'msw'
import type { UserResponse } from '../../features/users/api/userApi'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockUsers: UserResponse[] = [
  {
    id: 'user-1',
    email: 'cajero@test.com',
    fullName: 'Juan García',
    phone: null,
    roleCode: 'CASHIER',
    roleId: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    isActive: true,
    tenantId: 'tenant-1',
    branchId: null,
    createdAt: '2024-01-01T00:00:00Z',
  },
  {
    id: 'user-2',
    email: 'cocina@test.com',
    fullName: 'Pedro López',
    phone: null,
    roleCode: 'KITCHEN',
    roleId: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
    isActive: true,
    tenantId: 'tenant-1',
    branchId: null,
    createdAt: '2024-01-02T00:00:00Z',
  },
]

function buildPage(content: UserResponse[]) {
  return { content, totalElements: content.length, totalPages: 1, number: 0, size: 20 }
}

export const userHandlers = [
  http.get(`${BASE}/users`, ({ request }) => {
    const url = new URL(request.url)
    const search = url.searchParams.get('search')?.toLowerCase() ?? ''
    const filtered = search
      ? mockUsers.filter(
          (u) =>
            u.fullName.toLowerCase().includes(search) ||
            u.email.toLowerCase().includes(search)
        )
      : mockUsers
    return HttpResponse.json({ data: buildPage(filtered) }, { status: 200 })
  }),

  http.post(`${BASE}/users`, async ({ request }) => {
    const body = (await request.json()) as Record<string, string>
    const newUser: UserResponse = {
      id: 'user-new',
      email: body.email ?? '',
      fullName: body.fullName ?? '',
      phone: body.phone ?? null,
      roleId: body.roleId ?? 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
      roleCode: 'CASHIER',
      isActive: true,
      tenantId: 'tenant-1',
      branchId: null,
      createdAt: new Date().toISOString(),
    }
    return HttpResponse.json({ data: newUser }, { status: 201 })
  }),

  http.put(`${BASE}/users/:id`, async ({ params, request }) => {
    const body = (await request.json()) as Record<string, string>
    const existing = mockUsers.find((u) => u.id === params.id)
    if (!existing) return HttpResponse.json({ error: 'NOT_FOUND' }, { status: 404 })
    const updated: UserResponse = {
      ...existing,
      fullName: body.fullName ?? existing.fullName,
      phone: body.phone ?? existing.phone,
      roleId: body.roleId ?? existing.roleId,
    }
    return HttpResponse.json({ data: updated }, { status: 200 })
  }),

  http.delete(`${BASE}/users/:id`, () => {
    return new HttpResponse(null, { status: 204 })
  }),
]

export const userErrorHandlers = {
  usersServerError: () =>
    http.get(`${BASE}/users`, () =>
      HttpResponse.json({ error: 'INTERNAL_ERROR' }, { status: 500 })
    ),
}
