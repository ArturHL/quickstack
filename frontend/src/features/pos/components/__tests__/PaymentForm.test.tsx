import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import PaymentForm from '../PaymentForm'

const ORDER_TOTAL = 116.0

describe('PaymentForm', () => {
  it('renders total, amount input and quick buttons', () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    expect(screen.getByLabelText('total de orden')).toHaveTextContent('$116.00')
    expect(screen.getByLabelText('monto recibido')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /exacto/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /100 pesos/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /200 pesos/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /500 pesos/i })).toBeInTheDocument()
  })

  it('Registrar Pago button is disabled when input is empty', () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    expect(screen.getByRole('button', { name: /registrar pago/i })).toBeDisabled()
  })

  it('shows validation error when amount is less than total', async () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    await userEvent.type(screen.getByLabelText('monto recibido'), '50')

    expect(screen.getByText(/el monto debe ser mayor/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /registrar pago/i })).toBeDisabled()
  })

  it('sets exact amount when Exacto button is clicked', async () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /exacto/i }))

    const input = screen.getByLabelText('monto recibido') as HTMLInputElement
    expect(input.value).toBe('116')
  })

  it('sets 200 when $200 quick button is clicked', async () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))

    const input = screen.getByLabelText('monto recibido') as HTMLInputElement
    expect(input.value).toBe('200')
  })

  it('shows change when amount exceeds total', async () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))

    expect(screen.getByLabelText('cambio')).toHaveTextContent('$84.00')
  })

  it('calls onSubmit with the entered amount when valid', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={onSubmit} />)

    await userEvent.click(screen.getByRole('button', { name: /200 pesos/i }))
    await userEvent.click(screen.getByRole('button', { name: /registrar pago/i }))

    expect(onSubmit).toHaveBeenCalledWith(200)
  })

  it('disables submit button when isLoading is true', () => {
    renderWithProviders(<PaymentForm orderTotal={ORDER_TOTAL} onSubmit={vi.fn()} isLoading />)

    expect(screen.getByRole('button', { name: /procesando/i })).toBeDisabled()
  })
})
