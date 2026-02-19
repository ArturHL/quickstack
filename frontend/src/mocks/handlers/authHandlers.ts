import { http, HttpResponse } from 'msw'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1/auth`

export const authHandlers = [
  http.post(`${BASE}/login`, () =>
    HttpResponse.json(
      {
        accessToken: 'test-access-token',
        user: { id: '1', email: 'owner@test.com', fullName: 'Test Owner', role: 'OWNER', tenantId: 'tenant-1' },
      },
      { status: 200 }
    )
  ),

  http.post(`${BASE}/register`, () => new HttpResponse(null, { status: 201 })),

  http.post(`${BASE}/logout`, () => new HttpResponse(null, { status: 204 })),

  http.get(`${BASE}/me`, () =>
    HttpResponse.json(
      { id: '1', email: 'owner@test.com', fullName: 'Test Owner', role: 'OWNER', tenantId: 'tenant-1' },
      { status: 200 }
    )
  ),

  http.post(`${BASE}/refresh`, () =>
    HttpResponse.json({ accessToken: 'new-access-token' }, { status: 200 })
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
