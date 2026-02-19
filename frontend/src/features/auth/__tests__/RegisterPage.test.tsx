import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, delay } from 'msw'
import { server } from '../../../mocks/server'
import { registerHandlers } from '../../../mocks/handlers/authHandlers'
import { renderInRoutes } from '../../../test-utils/renderWithProviders'
import RegisterPage from '../RegisterPage'
import { useAuthStore } from '../../../stores/authStore'

function renderRegisterPage() {
  return renderInRoutes(
    [
      { path: '/register', element: <RegisterPage /> },
      { path: '/login', element: <div>Login page</div> },
    ],
    { initialRoute: '/register' }
  )
}

async function fillForm({
  fullName = 'Ana García',
  email = 'ana@test.com',
  password = 'SecurePass123!',
  confirmPassword = 'SecurePass123!',
} = {}) {
  const user = userEvent.setup()
  await user.type(screen.getByRole('textbox', { name: /nombre completo/i }), fullName)
  await user.type(screen.getByRole('textbox', { name: /email/i }), email)
  await user.type(screen.getByTestId('password-input'), password)
  await user.type(screen.getByTestId('confirm-password-input'), confirmPassword)
  return user
}

describe('RegisterPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null, isAuthenticated: false, isLoading: false })
  })

  it('renderiza todos los campos del formulario', () => {
    renderRegisterPage()
    expect(screen.getByRole('textbox', { name: /nombre completo/i })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /email/i })).toBeInTheDocument()
    expect(screen.getByTestId('password-input')).toBeInTheDocument()
    expect(screen.getByTestId('confirm-password-input')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear cuenta/i })).toBeInTheDocument()
  })

  it('muestra link de vuelta a login', () => {
    renderRegisterPage()
    expect(screen.getByRole('link', { name: /ya tienes cuenta/i })).toBeInTheDocument()
  })

  it('registro exitoso navega a /login', async () => {
    renderRegisterPage()
    await fillForm()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))
    await waitFor(() => {
      expect(screen.getByText(/login page/i)).toBeInTheDocument()
    })
  })

  it('muestra error de contraseñas no coinciden — sin llamar al server', async () => {
    renderRegisterPage()
    await fillForm({ confirmPassword: 'OtraPassword123!' })
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))
    expect(screen.getByText(/las contraseñas no coinciden/i)).toBeInTheDocument()
  })

  it('muestra error 409 — email ya registrado', async () => {
    server.use(registerHandlers.conflict())
    renderRegisterPage()
    await fillForm()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/este email ya está registrado/i)
    })
  })

  it('muestra error 400 — contraseña comprometida con mensaje del backend', async () => {
    server.use(registerHandlers.compromisedPassword())
    renderRegisterPage()
    await fillForm()
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }))
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/expuesta en brechas/i)
    })
  })

  it('muestra indicador de fortaleza al escribir la contraseña', async () => {
    renderRegisterPage()
    const user = userEvent.setup()
    await user.type(screen.getByTestId('password-input'), 'abc')
    expect(screen.getByText(/fortaleza: débil/i)).toBeInTheDocument()
  })

  it('deshabilita el botón durante el submit', async () => {
    const API_BASE = import.meta.env.VITE_API_BASE_URL as string
    server.use(
      http.post(`${API_BASE}/api/v1/auth/register`, async () => {
        await delay('infinite')
        return new HttpResponse(null)
      })
    )
    renderRegisterPage()
    await fillForm()
    const button = screen.getByRole('button', { name: /crear cuenta/i })
    const user = userEvent.setup()
    void user.click(button)
    await waitFor(() => expect(button).toBeDisabled())
  })
})
