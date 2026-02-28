import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductList from '../ProductList'
import { server } from '../../../../mocks/server'
import { http, HttpResponse } from 'msw'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

beforeEach(() => {
  localStorage.clear()
})

describe('ProductList', () => {
  it('shows loading spinner initially', () => {
    renderWithProviders(<ProductList />)
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('renders product list after loading', async () => {
    renderWithProviders(<ProductList />)

    await waitFor(() =>
      expect(screen.getByText('Café Americano')).toBeInTheDocument()
    )
    expect(screen.getByText('Sandwich Club')).toBeInTheDocument()
  })

  it('shows "Nuevo Producto" button', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /nuevo producto/i })).toBeInTheDocument()
  })

  it('filters products by search term', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())

    const searchInput = screen.getByRole('textbox', { name: /buscar producto/i })
    fireEvent.change(searchInput, { target: { value: 'Café' } })

    await waitFor(
      () => {
        expect(screen.queryByText('Sandwich Club')).not.toBeInTheDocument()
        expect(screen.getByText('Café Americano')).toBeInTheDocument()
      },
      { timeout: 3000 }
    )
  })

  it('shows delete confirmation dialog when delete is clicked', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())

    const deleteBtn = screen.getAllByRole('button', { name: /eliminar/i })[0]
    await userEvent.click(deleteBtn)

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar producto\?/i)).toBeInTheDocument()
  })

  it('cancels delete when cancel is clicked', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())

    await userEvent.click(screen.getAllByRole('button', { name: /eliminar/i })[0])
    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('shows error message on API failure', async () => {
    server.use(
      http.get(`${BASE}/products`, () =>
        HttpResponse.json({ error: 'INTERNAL_ERROR' }, { status: 500 })
      )
    )
    renderWithProviders(<ProductList />)

    await waitFor(() =>
      expect(screen.getByText(/error al cargar productos/i)).toBeInTheDocument()
    )
  })

  it('shows category names in table', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    expect(screen.getByText('Bebidas')).toBeInTheDocument()
    expect(screen.getByText('Comida')).toBeInTheDocument()
  })

  it('shows active chip for active products', async () => {
    renderWithProviders(<ProductList />)
    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    const activeChips = screen.getAllByText('Activo')
    expect(activeChips.length).toBeGreaterThan(0)
  })
})
