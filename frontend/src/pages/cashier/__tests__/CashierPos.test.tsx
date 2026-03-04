import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../test-utils/renderWithProviders'
import CashierPos from '../CashierPos'
import { useCartStore, initialCartState } from '../../../features/pos/stores/cartStore'
import { useBranchStore, initialBranchState } from '../../../features/pos/stores/branchStore'

function seedCart(serviceType: 'COUNTER' | 'TAKEOUT' = 'COUNTER') {
  useCartStore.setState({
    ...initialCartState,
    serviceType,
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
})

// ─── Layout ──────────────────────────────────────────────────────────────────

describe('CashierPos — layout split-screen', () => {
  it('renderiza el catálogo de productos en el panel izquierdo', async () => {
    renderWithProviders(<CashierPos />)

    await waitFor(() =>
      expect(screen.getByRole('tab', { name: /bebidas/i })).toBeInTheDocument()
    )
  })

  it('renderiza el toggle COUNTER/TAKEOUT en el panel derecho', () => {
    renderWithProviders(<CashierPos />)

    expect(screen.getByRole('button', { name: /mostrador/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /para llevar/i })).toBeInTheDocument()
  })

  it('muestra estado vacío cuando el carrito está vacío', () => {
    renderWithProviders(<CashierPos />)

    expect(screen.getByText(/selecciona productos del catálogo/i)).toBeInTheDocument()
  })

  it('muestra los items del carrito cuando hay productos', () => {
    seedCart()
    renderWithProviders(<CashierPos />)

    expect(screen.getByText('Café Americano')).toBeInTheDocument()
  })
})

// ─── Service type toggle ──────────────────────────────────────────────────────

describe('CashierPos — service type toggle', () => {
  it('inicializa con COUNTER si el carrito no tiene serviceType', () => {
    useCartStore.setState({ ...initialCartState, serviceType: null })
    renderWithProviders(<CashierPos />)

    expect(screen.getByRole('button', { name: /mostrador/i })).toBeInTheDocument()
  })

  it('cambia el serviceType al hacer click en Para llevar', async () => {
    renderWithProviders(<CashierPos />)

    await userEvent.click(screen.getByRole('button', { name: /para llevar/i }))

    expect(useCartStore.getState().serviceType).toBe('TAKEOUT')
  })
})

// ─── Customer validation ──────────────────────────────────────────────────────

describe('CashierPos — validación de cliente', () => {
  it('Enviar Orden deshabilitado cuando el carrito está vacío', () => {
    renderWithProviders(<CashierPos />)

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })

  it('Enviar Orden deshabilitado sin sucursal activa', () => {
    useBranchStore.setState({ ...initialBranchState, activeBranchId: null })
    seedCart()
    renderWithProviders(<CashierPos />)

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })

  it('Enviar Orden deshabilitado si nombre está vacío (COUNTER)', () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })

  it('Enviar Orden habilitado en COUNTER con nombre', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')

    expect(screen.getByRole('button', { name: /enviar orden/i })).not.toBeDisabled()
  })

  it('Enviar Orden deshabilitado en TAKEOUT sin teléfono', async () => {
    seedCart('TAKEOUT')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')
    await userEvent.type(screen.getByRole('textbox', { name: /calle y número exterior/i }), 'Calle 123')

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })

  it('Enviar Orden deshabilitado en TAKEOUT sin dirección', async () => {
    seedCart('TAKEOUT')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')
    await userEvent.type(screen.getByRole('textbox', { name: /teléfono del cliente/i }), '5551234567')

    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })
})

// ─── Customer autofill ────────────────────────────────────────────────────────

describe('CashierPos — autorellenar cliente por teléfono', () => {
  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('muestra cliente encontrado tras debounce al escribir teléfono', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /teléfono del cliente/i }), '555')
    act(() => { vi.advanceTimersByTime(300) })

    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
  })

  it('autorellenar nombre al clickear "Usar estos datos"', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /teléfono del cliente/i }), '555')
    act(() => { vi.advanceTimersByTime(300) })

    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: /usar datos de juan garcía/i }))

    expect(screen.getByRole('textbox', { name: /nombre del cliente/i })).toHaveValue('Juan García')
    expect(screen.getByText(/cliente vinculado/i)).toBeInTheDocument()
  })
})

// ─── COUNTER flow ─────────────────────────────────────────────────────────────

describe('CashierPos — flujo COUNTER completo', () => {
  it('al Enviar Orden aparece el formulario de cobro', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')
    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))

    await waitFor(() =>
      expect(screen.getByText('Cobrar')).toBeInTheDocument()
    )
    expect(screen.getByRole('button', { name: /registrar pago/i })).toBeInTheDocument()
  })

  it('al registrar pago muestra la confirmación del pedido', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')
    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))

    await waitFor(() => expect(screen.getByText('Cobrar')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))
    await userEvent.click(screen.getByRole('button', { name: /registrar pago/i }))

    await waitFor(() =>
      expect(screen.getByText('Pedido Completado')).toBeInTheDocument()
    )
  })

  it('"Nueva Venta" vuelve al panel de carrito vacío', async () => {
    seedCart('COUNTER')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Pedro')
    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))
    await waitFor(() => expect(screen.getByText('Cobrar')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))
    await userEvent.click(screen.getByRole('button', { name: /registrar pago/i }))
    await waitFor(() => expect(screen.getByText('Pedido Completado')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva venta/i }))

    await waitFor(() =>
      expect(screen.getByText(/selecciona productos del catálogo/i)).toBeInTheDocument()
    )
    expect(screen.getByRole('button', { name: /enviar orden/i })).toBeDisabled()
  })
})

// ─── TAKEOUT flow ─────────────────────────────────────────────────────────────

describe('CashierPos — flujo TAKEOUT completo', () => {
  it('completa el flujo de venta TAKEOUT con datos de cliente', async () => {
    seedCart('TAKEOUT')
    renderWithProviders(<CashierPos />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre del cliente/i }), 'Ana')
    await userEvent.type(screen.getByRole('textbox', { name: /teléfono del cliente/i }), '5559998877')
    await userEvent.type(screen.getByRole('textbox', { name: /calle y número exterior/i }), 'Av. Juárez 45')

    expect(screen.getByRole('button', { name: /enviar orden/i })).not.toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: /enviar orden/i }))

    await waitFor(() =>
      expect(screen.getByText('Cobrar')).toBeInTheDocument()
    )
  })
})
