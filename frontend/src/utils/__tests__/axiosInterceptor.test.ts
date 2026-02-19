import { describe, it, expect, beforeEach, vi } from 'vitest'
import { http, HttpResponse, delay } from 'msw'
import { server } from '../../mocks/server'
import axiosInstance from '../axiosInstance'
import { useAuthStore } from '../../stores/authStore'
import * as navUtils from '../imperativeNavigate'
import type { AuthUser } from '../../types/auth'

const BASE = import.meta.env.VITE_API_BASE_URL as string

const mockUser: AuthUser = {
  id: '1',
  email: 'owner@test.com',
  fullName: 'Test Owner',
  role: 'OWNER',
  tenantId: 'tenant-1',
}

describe('axiosInstance — interceptor de auth', () => {
  let navigateSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false, isLoading: false })
    navigateSpy = vi.spyOn(navUtils, 'imperativeNavigate').mockImplementation(() => {})
  })

  // ─── Request interceptor ──────────────────────────────────────────────────

  it('agrega Authorization Bearer si hay accessToken en el store', async () => {
    let capturedAuthHeader: string | null = null

    server.use(
      http.get(`${BASE}/api/test`, ({ request }) => {
        capturedAuthHeader = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      })
    )

    useAuthStore.getState().setAuth('my-token', mockUser)
    await axiosInstance.get('/api/test')

    expect(capturedAuthHeader).toBe('Bearer my-token')
    navigateSpy.mockRestore()
  })

  it('no agrega Authorization si no hay token', async () => {
    let capturedAuthHeader: string | null = null

    server.use(
      http.get(`${BASE}/api/test`, ({ request }) => {
        capturedAuthHeader = request.headers.get('Authorization')
        return HttpResponse.json({ ok: true })
      })
    )

    await axiosInstance.get('/api/test')

    expect(capturedAuthHeader).toBeNull()
    navigateSpy.mockRestore()
  })

  // ─── Response interceptor — refresh exitoso ───────────────────────────────

  it('401 → llama a refresh → reintenta con nuevo token → retorna respuesta correcta', async () => {
    let requestCount = 0

    server.use(
      http.get(`${BASE}/api/protected`, ({ request }) => {
        requestCount++
        const auth = request.headers.get('Authorization')
        // Primera llamada → 401; segunda (retry) con nuevo token → 200
        if (requestCount === 1 || auth === 'Bearer old-token') {
          return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ data: 'secret-data' })
      }),
      http.post(`${BASE}/api/v1/auth/refresh`, () =>
        HttpResponse.json({ accessToken: 'new-token' }, { status: 200 })
      )
    )

    useAuthStore.getState().setAuth('old-token', mockUser)
    const response = await axiosInstance.get('/api/protected')

    expect(response.data).toEqual({ data: 'secret-data' })
    expect(useAuthStore.getState().accessToken).toBe('new-token')
    navigateSpy.mockRestore()
  })

  it('refresh exitoso actualiza el accessToken en el store', async () => {
    server.use(
      http.get(`${BASE}/api/protected`, ({ request }) => {
        const auth = request.headers.get('Authorization')
        if (auth === 'Bearer old-token') {
          return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ ok: true })
      }),
      http.post(`${BASE}/api/v1/auth/refresh`, () =>
        HttpResponse.json({ accessToken: 'refreshed-token' }, { status: 200 })
      )
    )

    useAuthStore.getState().setAuth('old-token', mockUser)
    await axiosInstance.get('/api/protected')

    expect(useAuthStore.getState().accessToken).toBe('refreshed-token')
    navigateSpy.mockRestore()
  })

  // ─── Response interceptor — refresh fallido ───────────────────────────────

  it('refresh falla → clearAuth() + redirige a /login', async () => {
    server.use(
      http.get(`${BASE}/api/protected`, () =>
        HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
      ),
      http.post(`${BASE}/api/v1/auth/refresh`, () =>
        HttpResponse.json({ error: 'Session expired' }, { status: 401 })
      )
    )

    useAuthStore.getState().setAuth('expired-token', mockUser)

    await expect(axiosInstance.get('/api/protected')).rejects.toThrow()

    expect(useAuthStore.getState().isAuthenticated).toBe(false)
    expect(useAuthStore.getState().accessToken).toBeNull()
    expect(navigateSpy).toHaveBeenCalledWith('/login')
    navigateSpy.mockRestore()
  })

  it('no intenta refresh si la petición 401 es del propio endpoint de refresh', async () => {
    server.use(
      http.post(`${BASE}/api/v1/auth/refresh`, () =>
        HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
      )
    )

    useAuthStore.getState().setAuth('token', mockUser)

    await expect(axiosInstance.post('/api/v1/auth/refresh')).rejects.toMatchObject({
      response: { status: 401 },
    })

    // clearAuth NO debería llamarse — la petición de refresh falló con 401
    // pero el interceptor ignoró este caso (isRefreshEndpoint = true)
    // El store no se limpia porque el fallo es del refresh mismo (manejado por el caller)
    navigateSpy.mockRestore()
  })

  it('no reintenta si el request ya fue reintentado (_retry = true)', async () => {
    let callCount = 0

    server.use(
      http.get(`${BASE}/api/protected`, () => {
        callCount++
        return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
      }),
      http.post(`${BASE}/api/v1/auth/refresh`, () =>
        HttpResponse.json({ accessToken: 'new-token' }, { status: 200 })
      )
    )

    useAuthStore.getState().setAuth('token', mockUser)

    await expect(axiosInstance.get('/api/protected')).rejects.toThrow()

    // El request original + 1 reintento = 2 llamadas; no bucle infinito
    expect(callCount).toBeLessThanOrEqual(2)
    navigateSpy.mockRestore()
  })

  // ─── Cola de requests concurrentes ───────────────────────────────────────

  it('múltiples 401 concurrentes → solo UN refresh, todos los requests se reintentan', async () => {
    let getCount = 0
    let refreshCount = 0

    server.use(
      http.get(`${BASE}/api/protected`, () => {
        getCount++
        // Primeras 2 llamadas (originales) → 401; siguientes (retries) → 200
        if (getCount <= 2) {
          return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 })
        }
        return HttpResponse.json({ data: 'ok' })
      }),
      http.post(`${BASE}/api/v1/auth/refresh`, async () => {
        refreshCount++
        await delay(20) // Simula latencia para que el segundo request entre a la cola
        return HttpResponse.json({ accessToken: 'new-token' })
      })
    )

    useAuthStore.getState().setAuth('old-token', mockUser)

    const [r1, r2] = await Promise.all([
      axiosInstance.get('/api/protected'),
      axiosInstance.get('/api/protected'),
    ])

    expect(refreshCount).toBe(1)
    expect(r1.data).toEqual({ data: 'ok' })
    expect(r2.data).toEqual({ data: 'ok' })
    navigateSpy.mockRestore()
  })
})
