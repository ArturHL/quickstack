import { describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ServiceTypeSelector from '../ServiceTypeSelector'
import { useCartStore, initialCartState } from '../../stores/cartStore'

beforeEach(() => {
  sessionStorage.clear()
  useCartStore.setState(initialCartState)
})

describe('ServiceTypeSelector', () => {
  it('renders all four service type cards', () => {
    renderWithProviders(<ServiceTypeSelector />)

    expect(screen.getByRole('button', { name: /mesa/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /mostrador/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /delivery/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /para llevar/i })).toBeInTheDocument()
  })

  it('sets serviceType DINE_IN in cartStore when Mesa is selected', async () => {
    renderWithProviders(<ServiceTypeSelector />)

    await userEvent.click(screen.getByRole('button', { name: /mesa/i }))

    expect(useCartStore.getState().serviceType).toBe('DINE_IN')
  })

  it('sets serviceType COUNTER in cartStore when Mostrador is selected', async () => {
    renderWithProviders(<ServiceTypeSelector />)

    await userEvent.click(screen.getByRole('button', { name: /mostrador/i }))

    expect(useCartStore.getState().serviceType).toBe('COUNTER')
  })

  it('sets serviceType DELIVERY in cartStore when Delivery is selected', async () => {
    renderWithProviders(<ServiceTypeSelector />)

    await userEvent.click(screen.getByRole('button', { name: /delivery/i }))

    expect(useCartStore.getState().serviceType).toBe('DELIVERY')
  })

  it('sets serviceType TAKEOUT in cartStore when Para llevar is selected', async () => {
    renderWithProviders(<ServiceTypeSelector />)

    await userEvent.click(screen.getByRole('button', { name: /para llevar/i }))

    expect(useCartStore.getState().serviceType).toBe('TAKEOUT')
  })
})
