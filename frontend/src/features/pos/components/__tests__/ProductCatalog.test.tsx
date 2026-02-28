import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductCatalog from '../ProductCatalog'
import { server } from '../../../../mocks/server'
import { menuErrorHandlers, mockMenuResponse } from '../../../../mocks/handlers/menuHandlers'

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

  it('calls onProductClick with the clicked product', async () => {
    const handleProductClick = vi.fn()
    renderWithProviders(<ProductCatalog onProductClick={handleProductClick} />)

    await waitFor(() => expect(screen.getByText('Café Americano')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Café Americano'))

    expect(handleProductClick).toHaveBeenCalledWith(
      mockMenuResponse.categories[0].products[0]
    )
  })
})
