import { http, HttpResponse } from 'msw'

const BASE = '/api/v1/auth'

export const authHandlers = [
  http.post(`${BASE}/login`, () => {
    return HttpResponse.json(
      { accessToken: 'test-access-token', user: { id: '1', email: 'test@example.com', fullName: 'Test User', role: 'OWNER', tenantId: 'tenant-1' } },
      { status: 200 }
    )
  }),

  http.post(`${BASE}/register`, () => {
    return new HttpResponse(null, { status: 201 })
  }),

  http.post(`${BASE}/logout`, () => {
    return new HttpResponse(null, { status: 204 })
  }),

  http.get(`${BASE}/me`, () => {
    return HttpResponse.json(
      { id: '1', email: 'test@example.com', fullName: 'Test User', role: 'OWNER', tenantId: 'tenant-1' },
      { status: 200 }
    )
  }),

  http.post(`${BASE}/refresh`, () => {
    return HttpResponse.json(
      { accessToken: 'new-access-token' },
      { status: 200 }
    )
  }),

  http.post(`${BASE}/forgot-password`, () => {
    return new HttpResponse(null, { status: 200 })
  }),

  http.post(`${BASE}/reset-password`, () => {
    return new HttpResponse(null, { status: 200 })
  }),
]
