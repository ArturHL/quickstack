import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderInRoutes } from '../../../test-utils/renderWithProviders'
import CartPage from '../pages/CartPage'
import PaymentPage from '../pages/PaymentPage'
import OrderConfirmationPage from '../pages/OrderConfirmationPage'
import { useCartStore, initialCartState } from '../stores/cartStore'
import { useBranchStore, initialBranchState } from '../stores/branchStore'
import { usePosStore, initialPosState } from '../stores/posStore'

const allRoutes = [
  { path: '/pos/cart', element: <CartPage /> },
  { path: '/pos/payment', element: <PaymentPage /> },
  { path: '/pos/confirmation', element: <OrderConfirmationPage /> },
  { path: '/orders', element: <div>Lista de Órdenes</div> },
]

function seedCart(serviceType: 'COUNTER' | 'DINE_IN' | 'DELIVERY' | 'TAKEOUT') {
  useCartStore.setState({
    ...initialCartState,
    serviceType,
    tableId: serviceType === 'DINE_IN' ? 'table-1' : null,
    customerId: serviceType === 'DELIVERY' ? 'cust-1' : null,
    items: [
      {
        productId: 'prod-1',
        productName: 'Café Americano',
        quantity: 1,
        unitPrice: 100,
        selectedModifiers: [],
        lineTotal: 116,
      },
    ],
  })
}

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  useCartStore.setState(initialCartState)
  useBranchStore.setState({ ...initialBranchState, activeBranchId: 'branch-1' })
  usePosStore.setState(initialPosState)
})

describe('POS flow — Cart → Orden → Pago', () => {
  it('/pos/cart renders Cart with items and Enviar Orden button', () => {
    seedCart('COUNTER')
    renderInRoutes(allRoutes, { initialRoute: '/pos/cart' })

    expect(screen.getByText('Café Americano')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeInTheDocument()
  })

  it('COUNTER flow: Enviar Orden navigates to /pos/payment', async () => {
    seedCart('COUNTER')
    renderInRoutes(allRoutes, { initialRoute: '/pos/cart' })

    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))

    await waitFor(() =>
      expect(screen.getByText('Pago de Orden')).toBeInTheDocument()
    )
  })

  it('DINE_IN flow: Enviar Orden navigates to /orders', async () => {
    seedCart('DINE_IN')
    renderInRoutes(allRoutes, { initialRoute: '/pos/cart' })

    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))

    await waitFor(() =>
      expect(screen.getByText('Lista de Órdenes')).toBeInTheDocument()
    )
  })

  it('Enviar Orden is disabled when serviceType is null', () => {
    useCartStore.setState({
      ...initialCartState,
      serviceType: null,
      items: [{ productId: 'p1', productName: 'P', quantity: 1, unitPrice: 10, selectedModifiers: [], lineTotal: 10 }],
    })
    renderInRoutes(allRoutes, { initialRoute: '/pos/cart' })

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })

  it('/pos/payment renders PaymentForm with order total', async () => {
    usePosStore.setState({ currentOrderId: 'order-ready' })
    renderInRoutes(allRoutes, { initialRoute: '/pos/payment' })

    await waitFor(() =>
      expect(screen.getByText('Pago de Orden')).toBeInTheDocument()
    )
    expect(screen.getByRole('button', { name: /registrar pago/i })).toBeInTheDocument()
  })

  it('PaymentPage: registering payment navigates to /pos/confirmation', async () => {
    usePosStore.setState({ currentOrderId: 'order-ready' })
    renderInRoutes(allRoutes, { initialRoute: '/pos/payment' })

    await waitFor(() => expect(screen.getByText('Pago de Orden')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))
    await userEvent.click(screen.getByRole('button', { name: /registrar pago/i }))

    await waitFor(() =>
      expect(screen.getByText('Pedido Completado')).toBeInTheDocument()
    )
  })

  it('/pos/confirmation shows Nueva Venta button that navigates to /pos/new', async () => {
    renderInRoutes(
      [...allRoutes, { path: '/pos/new', element: <div>Tipo de Servicio</div> }],
      { initialRoute: '/pos/confirmation' }
    )

    expect(screen.getByText('Pedido Completado')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /nueva venta/i }))

    await waitFor(() =>
      expect(screen.getByText('Tipo de Servicio')).toBeInTheDocument()
    )
  })
})
