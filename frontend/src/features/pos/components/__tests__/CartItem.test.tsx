import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import CartItemComponent from '../CartItem'
import type { CartItem } from '../../types/Cart'

const simpleItem: CartItem = {
  productId: 'prod-1',
  productName: 'Café Americano',
  quantity: 2,
  unitPrice: 35.0,
  selectedModifiers: [],
  lineTotal: 70.0,
}

const itemWithModifiers: CartItem = {
  productId: 'prod-2',
  variantId: 'var-1',
  productName: 'Sandwich',
  variantName: 'Chico',
  quantity: 1,
  unitPrice: 85.0,
  selectedModifiers: [
    { modifierId: 'mod-1', modifierName: 'Sin cebolla', priceAdjustment: 0 },
    { modifierId: 'mod-2', modifierName: 'Extra queso', priceAdjustment: 10 },
  ],
  lineTotal: 95.0,
}

describe('CartItemComponent', () => {
  it('renders product name and line total', () => {
    renderWithProviders(
      <CartItemComponent item={simpleItem} index={0} onUpdateQty={vi.fn()} onRemove={vi.fn()} />
    )

    expect(screen.getByText('Café Americano')).toBeInTheDocument()
    expect(screen.getByText('$70.00')).toBeInTheDocument()
  })

  it('renders variant name when present', () => {
    renderWithProviders(
      <CartItemComponent item={itemWithModifiers} index={0} onUpdateQty={vi.fn()} onRemove={vi.fn()} />
    )

    expect(screen.getByText(/Sandwich — Chico/)).toBeInTheDocument()
  })

  it('renders modifier chips when present', () => {
    renderWithProviders(
      <CartItemComponent item={itemWithModifiers} index={0} onUpdateQty={vi.fn()} onRemove={vi.fn()} />
    )

    expect(screen.getByText('Sin cebolla')).toBeInTheDocument()
    expect(screen.getByText('Extra queso')).toBeInTheDocument()
  })

  it('calls onUpdateQty with incremented quantity when + is clicked', async () => {
    const handleUpdateQty = vi.fn()
    renderWithProviders(
      <CartItemComponent item={simpleItem} index={0} onUpdateQty={handleUpdateQty} onRemove={vi.fn()} />
    )

    await userEvent.click(screen.getByRole('button', { name: 'aumentar cantidad' }))

    expect(handleUpdateQty).toHaveBeenCalledWith(0, 3)
  })

  it('calls onUpdateQty with decremented quantity when - is clicked', async () => {
    const handleUpdateQty = vi.fn()
    renderWithProviders(
      <CartItemComponent item={simpleItem} index={0} onUpdateQty={handleUpdateQty} onRemove={vi.fn()} />
    )

    await userEvent.click(screen.getByRole('button', { name: 'disminuir cantidad' }))

    expect(handleUpdateQty).toHaveBeenCalledWith(0, 1)
  })

  it('calls onRemove with correct index when delete button is clicked', async () => {
    const handleRemove = vi.fn()
    renderWithProviders(
      <CartItemComponent item={simpleItem} index={2} onUpdateQty={vi.fn()} onRemove={handleRemove} />
    )

    await userEvent.click(screen.getByRole('button', { name: 'eliminar item' }))

    expect(handleRemove).toHaveBeenCalledWith(2)
  })
})
