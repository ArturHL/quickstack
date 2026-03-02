import { http, HttpResponse } from 'msw'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth`

const TEST_USER = {
  id: '1',
  email: 'owner@test.com',
  fullName: 'Test Owner',
  roleId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  tenantId: 'tenant-1',
  branchId: null,
}

export const authHandlers = [
  http.post(`${BASE}/login`, () =>
    HttpResponse.json(
      {
        data: {
          accessToken: 'test-access-token',
          tokenType: 'Bearer',
          expiresIn: 900,
          user: TEST_USER,
        },
        meta: { timestamp: '2026-01-01T00:00:00Z' },
      },
      { status: 200 }
    )
  ),

  http.post(`${BASE}/register`, () => new HttpResponse(null, { status: 201 })),

  http.post(`${BASE}/logout`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${BASE}/me`, () =>
    HttpResponse.json(
      { data: TEST_USER, meta: { timestamp: '2026-01-01T00:00:00Z' } },
      { status: 200 }
    )
  ),

  http.post(`${BASE}/refresh`, () =>
    HttpResponse.json(
      {
        data: {
          accessToken: 'new-access-token',
          tokenType: 'Bearer',
          expiresIn: 900,
          user: TEST_USER,
        },
        meta: { timestamp: '2026-01-01T00:00:00Z' },
      },
      { status: 200 }
    )
  ),

  http.post(`${BASE}/forgot-password`, () => new HttpResponse(null, { status: 200 })),

  http.post(`${BASE}/reset-password`, () => new HttpResponse(null, { status: 200 })),
]

// ─── Handler factories para override en tests ─────────────────────────────────

export const loginHandlers = {
  unauthorized: () =>
    http.post(`${BASE}/login`, () =>
      HttpResponse.json(
        { error: 'INVALID_CREDENTIALS', message: 'Email o contraseña incorrectos' },
        { status: 401 }
      )
    ),

  locked: (lockedUntil = new Date(Date.now() + 10 * 60_000).toISOString()) =>
    http.post(`${BASE}/login`, () =>
      HttpResponse.json(
        { error: 'ACCOUNT_LOCKED', message: 'Cuenta bloqueada' },
        { status: 423, headers: { 'X-Locked-Until': lockedUntil } }
      )
    ),

  rateLimited: (retryAfter = '60') =>
    http.post(`${BASE}/login`, () =>
      HttpResponse.json(
        { error: 'RATE_LIMITED', message: 'Demasiados intentos' },
        { status: 429, headers: { 'Retry-After': retryAfter } }
      )
    ),
}

export const registerHandlers = {
  conflict: () =>
    http.post(`${BASE}/register`, () =>
      HttpResponse.json(
        { error: 'EMAIL_ALREADY_EXISTS', message: 'Este email ya está registrado' },
        { status: 409 }
      )
    ),

  compromisedPassword: () =>
    http.post(`${BASE}/register`, () =>
      HttpResponse.json(
        { error: 'COMPROMISED_PASSWORD', message: 'Esta contraseña fue expuesta en brechas de datos. Elige otra.' },
        { status: 400 }
      )
    ),
}

export const refreshHandlers = {
  unauthorized: () =>
    http.post(`${BASE}/refresh`, () =>
      HttpResponse.json({ error: 'INVALID_TOKEN', message: 'Sesión expirada' }, { status: 401 })
    ),
}
