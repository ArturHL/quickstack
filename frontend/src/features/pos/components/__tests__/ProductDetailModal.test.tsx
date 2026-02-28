import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductDetailModal from '../ProductDetailModal'
import { useCartStore, initialCartState } from '../../stores/cartStore'
import type { MenuProductItem } from '../../types/Menu'

const simpleProduct: MenuProductItem = {
  id: 'prod-1',
  name: 'Café Americano',
  basePrice: 35.0,
  imageUrl: null,
  isAvailable: true,
  productType: 'SIMPLE',
  variants: [],
  modifierGroups: [],
}

const variantProduct: MenuProductItem = {
  id: 'prod-2',
  name: 'Sandwich',
  basePrice: 85.0,
  imageUrl: null,
  isAvailable: true,
  productType: 'VARIANT',
  variants: [
    { id: 'var-1', name: 'Chico', priceAdjustment: 0, effectivePrice: 85.0, isDefault: true, sortOrder: 1 },
    { id: 'var-2', name: 'Grande', priceAdjustment: 20.0, effectivePrice: 105.0, isDefault: false, sortOrder: 2 },
  ],
  modifierGroups: [],
}

const productWithRequiredModifier: MenuProductItem = {
  ...simpleProduct,
  id: 'prod-3',
  name: 'Bebida especial',
  modifierGroups: [
    {
      id: 'grp-1',
      name: 'Temperatura',
      minSelections: 1,
      maxSelections: 1,
      isRequired: true,
      modifiers: [
        { id: 'mod-hot', name: 'Caliente', priceAdjustment: 0, isDefault: false, sortOrder: 1 },
        { id: 'mod-cold', name: 'Frío', priceAdjustment: 0, isDefault: false, sortOrder: 2 },
      ],
    },
  ],
}

beforeEach(() => {
  sessionStorage.clear()
  useCartStore.setState(initialCartState)
})

describe('ProductDetailModal', () => {
  it('renders product name in dialog title', () => {
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={vi.fn()} />)

    expect(screen.getByRole('heading', { name: 'Café Americano' })).toBeInTheDocument()
  })

  it('shows VariantSelector for VARIANT products', () => {
    renderWithProviders(<ProductDetailModal product={variantProduct} open onClose={vi.fn()} />)

    expect(screen.getByText('Variante')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Chico/ })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Grande/ })).toBeInTheDocument()
  })

  it('does NOT show VariantSelector for SIMPLE products', () => {
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={vi.fn()} />)

    expect(screen.queryByText('Variante')).not.toBeInTheDocument()
  })

  it('renders modifier groups', () => {
    renderWithProviders(<ProductDetailModal product={productWithRequiredModifier} open onClose={vi.fn()} />)

    expect(screen.getByText('Temperatura')).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Caliente/ })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: /Frío/ })).toBeInTheDocument()
  })

  it('increments quantity when "+" is clicked', async () => {
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: 'aumentar cantidad' }))

    expect(screen.getByLabelText('cantidad')).toHaveTextContent('2')
  })

  it('decrements quantity but not below 1', async () => {
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={vi.fn()} />)

    // Try to decrement from 1
    await userEvent.click(screen.getByRole('button', { name: 'disminuir cantidad' }))

    expect(screen.getByLabelText('cantidad')).toHaveTextContent('1')
  })

  it('updates lineTotal in real time when quantity changes', async () => {
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={vi.fn()} />)

    // Initial: 35.00 × 1 = 35.00
    expect(screen.getByLabelText('total del item')).toHaveTextContent('Total: $35.00')

    await userEvent.click(screen.getByRole('button', { name: 'aumentar cantidad' }))

    // After increment: 35.00 × 2 = 70.00
    expect(screen.getByLabelText('total del item')).toHaveTextContent('Total: $70.00')
  })

  it('shows validation error when VARIANT product but no variant selected', async () => {
    // Create a variant product where default variant is pre-selected, then clear selection
    const noDefaultVariant: MenuProductItem = {
      ...variantProduct,
      variants: [
        { id: 'var-1', name: 'Chico', priceAdjustment: 0, effectivePrice: 85.0, isDefault: false, sortOrder: 1 },
      ],
    }
    renderWithProviders(<ProductDetailModal product={noDefaultVariant} open onClose={vi.fn()} />)

    await userEvent.click(screen.getByRole('button', { name: /Agregar al Carrito/i }))

    expect(screen.getByText('Selecciona una variante')).toBeInTheDocument()
  })

  it('shows validation error when required modifier group not satisfied', async () => {
    renderWithProviders(
      <ProductDetailModal product={productWithRequiredModifier} open onClose={vi.fn()} />
    )

    await userEvent.click(screen.getByRole('button', { name: /Agregar al Carrito/i }))

    expect(screen.getByText('Selecciona al menos 1')).toBeInTheDocument()
  })

  it('calls addItem with correct data on valid submit', async () => {
    const onClose = vi.fn()
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={onClose} />)

    await userEvent.click(screen.getByRole('button', { name: /Agregar al Carrito/i }))

    const items = useCartStore.getState().items
    expect(items).toHaveLength(1)
    expect(items[0].productId).toBe('prod-1')
    expect(items[0].productName).toBe('Café Americano')
    expect(items[0].quantity).toBe(1)
    expect(items[0].lineTotal).toBe(35.0)
  })

  it('closes modal after adding to cart', async () => {
    const onClose = vi.fn()
    renderWithProviders(<ProductDetailModal product={simpleProduct} open onClose={onClose} />)

    await userEvent.click(screen.getByRole('button', { name: /Agregar al Carrito/i }))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
  })
})
