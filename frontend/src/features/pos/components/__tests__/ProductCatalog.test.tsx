import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductCatalog from '../ProductCatalog'
import { server } from '../../../../mocks/server'
import { menuErrorHandlers } from '../../../../mocks/handlers/menuHandlers'
import { useCartStore, initialCartState } from '../../stores/cartStore'

beforeEach(() => {
  sessionStorage.clear()
  useCartStore.setState(initialCartState)
})

describe('ProductCatalog', () => {
  it('shows loading spinner while fetching', () => {
    renderWithProviders(<ProductCatalog />)

    expect(screen.getByRole('status', { name: 'Cargando catálogo' })).toBeInTheDocument()
  })

  it('shows error alert when fetch fails', async () => {
    server.use(menuErrorHandlers.serverError())
    renderWithProviders(<ProductCatalog />)

    await waitFor(
      () => expect(screen.getByRole('alert')).toBeInTheDocument(),
      { timeout: 10000 }
    )
    expect(screen.getByText(/error al cargar el catálogo/i)).toBeInTheDocument()
  })

  it('renders category tabs after loading', async () => {
    renderWithProviders(<ProductCatalog />)

    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())
    expect(screen.getByText('Alimentos')).toBeInTheDocument()
  })

  it('renders products of the first category by default', async () => {
    renderWithProviders(<ProductCatalog />)

    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    expect(screen.getByText('Cappuccino')).toBeInTheDocument()
    // Products from Alimentos category should not be visible
    expect(screen.queryByText('Sandwich')).not.toBeInTheDocument()
  })

  it('switches to a different category on tab click', async () => {
    renderWithProviders(<ProductCatalog />)

    await waitFor(() => expect(screen.getByText('Alimentos')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Alimentos'))

    expect(screen.getByText('Sandwich')).toBeInTheDocument()
    expect(screen.queryByText('Café Americano')).not.toBeInTheDocument()
  })

  it('clicking a product card opens ProductDetailModal', async () => {
    renderWithProviders(<ProductCatalog />)

    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Café Americano'))

    // Modal dialog should be visible with the product name as title
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Café Americano' })).toBeInTheDocument()
  })

  it('closing the modal hides the dialog', async () => {
    renderWithProviders(<ProductCatalog />)

    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Café Americano'))

    // Modal open
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    // Close via Cancelar button
    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }))

    await waitFor(() =>
      expect(screen.queryByRole('heading', { name: 'Café Americano' })).not.toBeInTheDocument()
    )
  })
})
