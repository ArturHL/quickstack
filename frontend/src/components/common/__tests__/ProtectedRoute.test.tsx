import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material/styles'
import theme from '../../../theme/theme'
import ProtectedRoute from '../ProtectedRoute'
import LoginPage from '../../../features/auth/LoginPage'
import { useAuthStore } from '../../../stores/authStore'

const routerFutureFlags = { v7_startTransition: true, v7_relativeSplatPath: true } as const

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })
}

function renderWithRoutes(initialRoute = '/dashboard') {
  const testQC = createTestQueryClient()

  return render(
    <MemoryRouter initialEntries={[initialRoute]} future={routerFutureFlags}>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={testQC}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<div>Register</div>} />
            <Route path="/forgot-password" element={<div>Forgot Password</div>} />
            <Route element={<ProtectedRoute />}>
              <Route path="/dashboard" element={<div>Dashboard protegido</div>} />
              <Route path="/reports" element={<div>Reports protegido</div>} />
              <Route path="/settings" element={<div>Settings protegido</div>} />
            </Route>
          </Routes>
        </QueryClientProvider>
      </ThemeProvider>
    </MemoryRouter>
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false, isLoading: false })
  })

  it('usuario no autenticado en /dashboard → redirigido a /login', () => {
    renderWithRoutes('/dashboard')
    expect(screen.queryByText('Dashboard protegido')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()
  })

  it('usuario autenticado en /dashboard → ve el contenido protegido', () => {
    useAuthStore.setState({
      accessToken: 'token',
      user: { id: '1', email: 'owner@test.com', fullName: 'Owner', role: 'OWNER', tenantId: 't1' },
      isAuthenticated: true,
      isLoading: false,
    })
    renderWithRoutes('/dashboard')
    expect(screen.getByText('Dashboard protegido')).toBeInTheDocument()
  })

  it('usuario no autenticado en /settings → redirigido a /login', () => {
    renderWithRoutes('/settings')
    expect(screen.queryByText('Settings protegido')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()
  })

  it('muestra spinner si isLoading es true', () => {
    useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false, isLoading: true })
    renderWithRoutes('/dashboard')
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard protegido')).not.toBeInTheDocument()
  })

  it('usuario autenticado en /login → redirigido a /dashboard (LoginPage maneja este caso)', () => {
    useAuthStore.setState({
      accessToken: 'token',
      user: { id: '1', email: 'owner@test.com', fullName: 'Owner', role: 'OWNER', tenantId: 't1' },
      isAuthenticated: true,
      isLoading: false,
    })
    renderWithRoutes('/login')
    expect(screen.getByText('Dashboard protegido')).toBeInTheDocument()
  })

  it('post-login redirige a la URL original (/reports) guardada por ProtectedRoute', async () => {
    // 1. Renderizar con usuario en /reports (no autenticado)
    //    ProtectedRoute guarda state.from = { pathname: '/reports' }
    //    y redirige a /login
    renderWithRoutes('/reports')
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()

    // 2. Hacer login — MSW default handler devuelve éxito
    const user = userEvent.setup()
    await user.type(screen.getByRole('textbox', { name: /email/i }), 'owner@test.com')
    await user.type(screen.getByTestId('password-input'), 'SecurePass123!')
    await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))

    // 3. useLogin lee state.from → /reports y navega ahí
    await waitFor(() => {
      expect(screen.getByText('Reports protegido')).toBeInTheDocument()
    })
  })

  it('renderiza el Outlet correcto para cada ruta autenticada', () => {
    useAuthStore.setState({
      accessToken: 'token',
      user: { id: '1', email: 'owner@test.com', fullName: 'Owner', role: 'OWNER', tenantId: 't1' },
      isAuthenticated: true,
      isLoading: false,
    })
    renderWithRoutes('/settings')
    expect(screen.getByText('Settings protegido')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard protegido')).not.toBeInTheDocument()
  })
})
