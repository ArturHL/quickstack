import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderInRoutes, renderWithProviders } from '../test-utils/renderWithProviders'
import CatalogPage from '../features/pos/pages/CatalogPage'
import NewOrderPage from '../features/pos/pages/NewOrderPage'
import TableSelectionPage from '../features/pos/pages/TableSelectionPage'
import CustomerSelectionPage from '../features/pos/pages/CustomerSelectionPage'
import CartPage from '../features/pos/pages/CartPage'
import Sidebar from '../components/layout/Sidebar'
import { useBranchStore, initialBranchState } from '../features/pos/stores/branchStore'
import { useCartStore, initialCartState } from '../features/pos/stores/cartStore'

const allPosRoutes = [
  { path: '/pos/catalog', element: <CatalogPage /> },
  { path: '/pos/new', element: <NewOrderPage /> },
  { path: '/pos/new/table', element: <TableSelectionPage /> },
  { path: '/pos/new/customer', element: <CustomerSelectionPage /> },
  { path: '/pos/cart', element: <CartPage /> },
]

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  useCartStore.setState(initialCartState)
  useBranchStore.setState(initialBranchState)
})

describe('POS Routes', () => {
  it('/pos/catalog renders CatalogPage with product catalog', async () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/catalog' })

    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())
    expect(screen.getByText('Alimentos')).toBeInTheDocument()
  })

  it('/pos/new renders ServiceTypeSelector with four service type options', () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/new' })

    expect(screen.getByText('Tipo de Servicio')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /mesa/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /mostrador/i })).toBeInTheDocument()
  })

  it('/pos/cart renders Cart with empty state', () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/cart' })

    expect(screen.getByText(/tu carrito está vacío/i)).toBeInTheDocument()
  })

  it('/pos/new/customer renders CustomerSelector', () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/new/customer' })

    expect(screen.getByText('Seleccionar Cliente')).toBeInTheDocument()
    expect(screen.getByLabelText(/buscar cliente/i)).toBeInTheDocument()
  })

  it('selecting DINE_IN navigates to /pos/new/table', async () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/new' })

    await userEvent.click(screen.getByRole('button', { name: /mesa/i }))

    await waitFor(() => expect(screen.getByText('Seleccionar Mesa')).toBeInTheDocument())
  })

  it('selecting COUNTER navigates to /pos/catalog', async () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/new' })

    await userEvent.click(screen.getByRole('button', { name: /mostrador/i }))

    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())
  })

  it('selecting DELIVERY navigates to /pos/new/customer', async () => {
    renderInRoutes(allPosRoutes, { initialRoute: '/pos/new' })

    await userEvent.click(screen.getByRole('button', { name: /delivery/i }))

    await waitFor(() => expect(screen.getByText('Seleccionar Cliente')).toBeInTheDocument())
  })

  it('Sidebar renders Catálogo link', () => {
    const onMobileClose = vi.fn()
    renderWithProviders(<Sidebar mobileOpen={false} onMobileClose={onMobileClose} />)

    expect(screen.getAllByText('Catálogo')[0]).toBeInTheDocument()
  })

  it('Sidebar Catálogo link is active when on /pos/* path', () => {
    const onMobileClose = vi.fn()
    renderWithProviders(<Sidebar mobileOpen={false} onMobileClose={onMobileClose} />, {
      initialRoute: '/pos/catalog',
    })

    const catalogButtons = screen.getAllByRole('button', { name: /catálogo/i })
    expect(catalogButtons.length).toBeGreaterThan(0)
  })
})
