import { describe, it, expect, beforeEach } from 'vitest'
import { useAuthStore } from '../authStore'
import type { AuthUser } from '../../types/auth'

const mockUser: AuthUser = {
  id: '1',
  email: 'owner@test.com',
  fullName: 'Test Owner',
  role: 'OWNER',
  tenantId: 'tenant-abc',
}

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      accessToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
    })
  })

  it('tiene el estado inicial correcto', () => {
    const state = useAuthStore.getState()
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(state.isAuthenticated).toBe(false)
    expect(state.isLoading).toBe(false)
  })

  it('setAuth guarda token y usuario, y activa isAuthenticated', () => {
    useAuthStore.getState().setAuth('test-token', mockUser)
    const state = useAuthStore.getState()
    expect(state.accessToken).toBe('test-token')
    expect(state.user).toEqual(mockUser)
    expect(state.isAuthenticated).toBe(true)
    expect(state.isLoading).toBe(false)
  })

  it('clearAuth resetea todo al estado inicial', () => {
    useAuthStore.getState().setAuth('test-token', mockUser)
    useAuthStore.getState().clearAuth()
    const state = useAuthStore.getState()
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(state.isAuthenticated).toBe(false)
    expect(state.isLoading).toBe(false)
  })

  it('setLoading actualiza isLoading correctamente', () => {
    useAuthStore.getState().setLoading(true)
    expect(useAuthStore.getState().isLoading).toBe(true)
    useAuthStore.getState().setLoading(false)
    expect(useAuthStore.getState().isLoading).toBe(false)
  })

  it('el store no persiste en localStorage', () => {
    useAuthStore.getState().setAuth('token-secreto', mockUser)
    expect(localStorage.getItem('auth')).toBeNull()
    expect(sessionStorage.getItem('auth')).toBeNull()
  })
})
