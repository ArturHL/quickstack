import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { server } from '../../../mocks/server'
import { loginHandlers } from '../../../mocks/handlers/authHandlers'
import { renderInRoutes } from '../../../test-utils/renderWithProviders'
import LoginPage from '../LoginPage'
import { useAuthStore } from '../../../stores/authStore'

function renderLoginPage({ initialRoute = '/login' } = {}) {
  return renderInRoutes(
    [
      { path: '/login', element: <LoginPage /> },
      { path: '/dashboard', element: <div>Dashboard</div> },
      { path: '/register', element: <div>Register</div> },
      { path: '/forgot-password', element: <div>Forgot Password</div> },
    ],
    { initialRoute }
  )
}

async function fillAndSubmit(email = 'owner@test.com', password = 'SecurePass123!') {
  const user = userEvent.setup()
  await user.type(screen.getByRole('textbox', { name: /email/i }), email)
  await user.type(screen.getByTestId('password-input'), password)
  await user.click(screen.getByRole('button', { name: /iniciar sesión/i }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false, isLoading: false })
  })

  it('renderiza los campos de email y contraseña', () => {
    renderLoginPage()
    expect(screen.getByRole('textbox', { name: /email/i })).toBeInTheDocument()
    expect(screen.getByTestId('password-input')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument()
  })

  it('muestra links a forgot-password y registro', () => {
    renderLoginPage()
    expect(screen.getByRole('link', { name: /olvidaste/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /crear cuenta/i })).toBeInTheDocument()
  })

  it('redirige a /dashboard si ya está autenticado', () => {
    useAuthStore.setState({
      accessToken: 'token',
      user: { id: '1', email: 'owner@test.com', fullName: 'Owner', role: 'OWNER', tenantId: 't1' },
      isAuthenticated: true,
      isLoading: false,
    })
    renderLoginPage()
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('muestra el banner de registro exitoso cuando ?registered=true', () => {
    renderLoginPage({ initialRoute: '/login?registered=true' })
    expect(screen.getByText(/cuenta creada/i)).toBeInTheDocument()
  })

  it('submit exitoso redirige a /dashboard', async () => {
    renderLoginPage()
    await fillAndSubmit()
    await waitFor(() => {
      expect(screen.getByText('Dashboard')).toBeInTheDocument()
    })
  })

  it('deshabilita el botón durante el submit', async () => {
    const API_BASE = import.meta.env.VITE_API_BASE_URL as string
    server.use(
      http.post(`${API_BASE}/api/v1/auth/login`, async () => {
        await delay('infinite')
        return new HttpResponse(null)
      })
    )
    renderLoginPage()
    const user = userEvent.setup()
    await user.type(screen.getByRole('textbox', { name: /email/i }), 'owner@test.com')
    await user.type(screen.getByTestId('password-input'), 'password')
    const button = screen.getByRole('button', { name: /iniciar sesión/i })
    void user.click(button)
    await waitFor(() => expect(button).toBeDisabled())
  })

  it('muestra error 401 — credenciales incorrectas', async () => {
    server.use(loginHandlers.unauthorized())
    renderLoginPage()
    await fillAndSubmit()
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/email o contraseña incorrectos/i)
    })
  })

  it('muestra error 423 — cuenta bloqueada con tiempo restante', async () => {
    const lockedUntil = new Date(Date.now() + 5 * 60_000).toISOString()
    server.use(loginHandlers.locked(lockedUntil))
    renderLoginPage()
    await fillAndSubmit()
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/cuenta bloqueada/i)
    })
  })

  it('muestra error 429 — demasiados intentos con Retry-After', async () => {
    server.use(loginHandlers.rateLimited('30'))
    renderLoginPage()
    await fillAndSubmit()
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/demasiados intentos/i)
      expect(screen.getByRole('alert')).toHaveTextContent('30')
    })
  })

  it('toggle de visibilidad de contraseña funciona', async () => {
    renderLoginPage()
    const user = userEvent.setup()
    const passwordField = screen.getByTestId('password-input')
    expect(passwordField).toHaveAttribute('type', 'password')
    await user.click(screen.getByRole('button', { name: /mostrar contraseña/i }))
    expect(passwordField).toHaveAttribute('type', 'text')
  })
})
