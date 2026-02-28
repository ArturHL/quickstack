import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductCard from '../ProductCard'
import type { MenuProductItem } from '../../types/Menu'

const availableProduct: MenuProductItem = {
  id: 'prod-1',
  name: 'Café Americano',
  basePrice: 35.0,
  imageUrl: null,
  isAvailable: true,
  productType: 'SIMPLE',
  variants: [],
  modifierGroups: [],
}

const unavailableProduct: MenuProductItem = {
  ...availableProduct,
  id: 'prod-2',
  name: 'Cappuccino',
  isAvailable: false,
}

const productWithImage: MenuProductItem = {
  ...availableProduct,
  id: 'prod-3',
  name: 'Sandwich',
  imageUrl: 'https://example.com/sandwich.jpg',
}

describe('ProductCard', () => {
  it('renders product name and price', () => {
    renderWithProviders(<ProductCard product={availableProduct} onClick={vi.fn()} />)

    expect(screen.getByText('Café Americano')).toBeInTheDocument()
    expect(screen.getByText('$35.00')).toBeInTheDocument()
  })

  it('calls onClick when available product is clicked', async () => {
    const handleClick = vi.fn()
    renderWithProviders(<ProductCard product={availableProduct} onClick={handleClick} />)

    await userEvent.click(screen.getByText('Café Americano'))

    expect(handleClick).toHaveBeenCalledOnce()
  })

  it('shows "No disponible" overlay when product is unavailable', () => {
    renderWithProviders(<ProductCard product={unavailableProduct} onClick={vi.fn()} />)

    expect(screen.getByText('No disponible')).toBeInTheDocument()
  })

  it('card button is disabled when product is unavailable', () => {
    renderWithProviders(<ProductCard product={unavailableProduct} onClick={vi.fn()} />)

    // Disabled button prevents clicks in real browsers
    expect(screen.getByRole('button')).toBeDisabled()
  })

  it('shows placeholder box when imageUrl is null', () => {
    renderWithProviders(<ProductCard product={availableProduct} onClick={vi.fn()} />)

    expect(screen.getByLabelText('sin imagen')).toBeInTheDocument()
  })

  it('renders image when imageUrl is provided', () => {
    renderWithProviders(<ProductCard product={productWithImage} onClick={vi.fn()} />)

    const img = screen.getByRole('img', { name: 'Sandwich' })
    expect(img).toHaveAttribute('src', 'https://example.com/sandwich.jpg')
  })
})
