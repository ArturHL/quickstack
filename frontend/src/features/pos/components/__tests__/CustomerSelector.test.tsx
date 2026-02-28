import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import CustomerSelector from '../CustomerSelector'
import { useCartStore, initialCartState } from '../../stores/cartStore'

beforeEach(() => {
  sessionStorage.clear()
  useCartStore.setState(initialCartState)
  vi.useFakeTimers({ shouldAdvanceTime: true })
})

afterEach(() => {
  vi.useRealTimers()
})

describe('CustomerSelector', () => {
  it('renders search input and create button', () => {
    renderWithProviders(<CustomerSelector />)

    expect(screen.getByLabelText(/buscar cliente/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /crear cliente nuevo/i })).toBeInTheDocument()
  })

  it('does not show results when search is empty', () => {
    renderWithProviders(<CustomerSelector />)

    expect(screen.queryByRole('list')).not.toBeInTheDocument()
  })

  it('shows customer list after debounce when typing', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.type(screen.getByLabelText(/buscar cliente/i), 'Juan')
    vi.advanceTimersByTime(300)

    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
  })

  it('shows all matching customers in results', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.type(screen.getByLabelText(/buscar cliente/i), 'a')
    vi.advanceTimersByTime(300)

    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    expect(screen.getByText('María López')).toBeInTheDocument()
  })

  it('saves customerId in cartStore when customer is selected', async () => {
    useCartStore.setState({ ...initialCartState, serviceType: 'DELIVERY' })
    renderWithProviders(<CustomerSelector />)

    await userEvent.type(screen.getByLabelText(/buscar cliente/i), 'Juan')
    vi.advanceTimersByTime(300)

    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    await userEvent.click(screen.getByText('Juan García'))

    expect(useCartStore.getState().customerId).toBe('cust-1')
  })

  it('opens create customer dialog when button is clicked', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.click(screen.getByRole('button', { name: /crear cliente nuevo/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Crear Cliente Nuevo' })).toBeInTheDocument()
  })

  it('shows name and phone fields in create dialog', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.click(screen.getByRole('button', { name: /crear cliente nuevo/i }))

    expect(screen.getByLabelText(/nombre del cliente/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/teléfono del cliente/i)).toBeInTheDocument()
  })

  it('create button is disabled when both name and phone are empty', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.click(screen.getByRole('button', { name: /crear cliente nuevo/i }))

    const createBtn = screen.getByRole('button', { name: /^crear$/i })
    expect(createBtn).toBeDisabled()
  })

  it('closes dialog when Cancelar is clicked', async () => {
    renderWithProviders(<CustomerSelector />)

    await userEvent.click(screen.getByRole('button', { name: /crear cliente nuevo/i }))
    expect(screen.getByRole('dialog')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('creates customer and saves customerId in cartStore on successful submit', async () => {
    useCartStore.setState({ ...initialCartState, serviceType: 'DELIVERY' })
    renderWithProviders(<CustomerSelector />)

    await userEvent.click(screen.getByRole('button', { name: /crear cliente nuevo/i }))
    await userEvent.type(screen.getByLabelText(/nombre del cliente/i), 'Nuevo Cliente')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() => expect(useCartStore.getState().customerId).toBe('cust-new'))
  })
})
