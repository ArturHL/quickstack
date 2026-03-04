import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import UserList from '../UserList'

beforeEach(() => {
  localStorage.clear()
})

describe('UserList', () => {
  it('renders user list after loading', async () => {
    renderWithProviders(<UserList />)

    await waitFor(() =>
      expect(screen.getByText('Juan García')).toBeInTheDocument()
    )
    expect(screen.getByText('Pedro López')).toBeInTheDocument()
  })

  it('shows Usuarios heading', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    expect(screen.getByRole('heading', { name: /usuarios/i })).toBeInTheDocument()
  })

  it('shows search input', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    expect(screen.getByRole('textbox', { name: /buscar usuario/i })).toBeInTheDocument()
  })

  it('shows skeleton (loading state) on initial render', () => {
    renderWithProviders(<UserList />)
    // During loading, a CircularProgress is shown
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('filters users by search', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.type(screen.getByRole('textbox', { name: /buscar usuario/i }), 'Juan')

    await waitFor(() =>
      expect(screen.queryByText('Pedro López')).not.toBeInTheDocument()
    )
    expect(screen.getByText('Juan García')).toBeInTheDocument()
  })

  it('opens create dialog on "+ Nuevo Usuario" click', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nuevo usuario/i }))

    expect(screen.getByRole('heading', { name: /nuevo usuario/i })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /email nuevo usuario/i })).toBeInTheDocument()
  })

  it('creates user and closes dialog on success', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nuevo usuario/i }))

    await userEvent.type(screen.getByRole('textbox', { name: /email nuevo usuario/i }), 'nuevo@test.com')
    await userEvent.type(screen.getByLabelText(/contraseña nuevo usuario/i), 'SecurePass1234')
    await userEvent.type(screen.getByRole('textbox', { name: /nombre nuevo usuario/i }), 'Nuevo Cajero')

    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: /nuevo usuario/i })).not.toBeInTheDocument()
    )
  })

  it('opens edit dialog on edit icon click', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar juan garcía/i }))

    expect(screen.getByText('Editar Usuario')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre usuario/i })).toHaveValue('Juan García')
  })

  it('saves user edit and closes dialog on success', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar juan garcía/i }))

    const nameInput = screen.getByRole('textbox', { name: /nombre usuario/i })
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Juan García Updated')

    await userEvent.click(screen.getByRole('button', { name: /guardar/i }))

    await waitFor(() =>
      expect(screen.queryByText('Editar Usuario')).not.toBeInTheDocument()
    )
  })

  it('opens delete confirmation on delete icon click', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar juan garcía/i }))

    expect(screen.getByRole('heading', { name: /eliminar usuario/i })).toBeInTheDocument()
    expect(screen.getByText(/perderá acceso al sistema/i)).toBeInTheDocument()
  })

  it('deletes user and closes confirmation on success', async () => {
    renderWithProviders(<UserList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar juan garcía/i }))

    await userEvent.click(screen.getByRole('button', { name: /^eliminar$/i }))

    await waitFor(() =>
      expect(screen.queryByText('¿Eliminar usuario?')).not.toBeInTheDocument()
    )
  })
})
