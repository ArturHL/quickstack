import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import VariantSelector from '../VariantSelector'
import type { MenuVariantItem } from '../../types/Menu'

const variants: MenuVariantItem[] = [
  { id: 'var-1', name: 'Chico', priceAdjustment: 0, effectivePrice: 50.0, isDefault: true, sortOrder: 1 },
  { id: 'var-2', name: 'Grande', priceAdjustment: 20.0, effectivePrice: 70.0, isDefault: false, sortOrder: 2 },
  { id: 'var-3', name: 'Extra Grande', priceAdjustment: 40.0, effectivePrice: 90.0, isDefault: false, sortOrder: 3 },
]

describe('VariantSelector', () => {
  it('renders all variant names', () => {
    renderWithProviders(<VariantSelector variants={variants} selectedId={null} onChange={vi.fn()} />)

    expect(screen.getByText(/Chico/)).toBeInTheDocument()
    expect(screen.getByText(/Extra Grande/)).toBeInTheDocument()
    // Both "Grande" and "Extra Grande" labels are present
    expect(screen.getAllByText(/Grande/).length).toBeGreaterThanOrEqual(2)
  })

  it('displays effective price for each variant', () => {
    renderWithProviders(<VariantSelector variants={variants} selectedId={null} onChange={vi.fn()} />)

    expect(screen.getByText(/\$50\.00/)).toBeInTheDocument()
    expect(screen.getByText(/\$70\.00/)).toBeInTheDocument()
    expect(screen.getByText(/\$90\.00/)).toBeInTheDocument()
  })

  it('marks the matching variant as selected', () => {
    renderWithProviders(<VariantSelector variants={variants} selectedId="var-2" onChange={vi.fn()} />)

    // Use /^Grande/ to match "Grande — $70.00" but not "Extra Grande — $90.00"
    const grandeRadio = screen.getByRole('radio', { name: /^Grande/ })
    expect(grandeRadio).toBeChecked()
  })

  it('no radio is checked when selectedId is null', () => {
    renderWithProviders(<VariantSelector variants={variants} selectedId={null} onChange={vi.fn()} />)

    const radios = screen.getAllByRole('radio')
    radios.forEach((radio) => expect(radio).not.toBeChecked())
  })

  it('calls onChange with variant id when a variant is selected', async () => {
    const handleChange = vi.fn()
    renderWithProviders(<VariantSelector variants={variants} selectedId="var-1" onChange={handleChange} />)

    await userEvent.click(screen.getByRole('radio', { name: /^Grande/ }))

    expect(handleChange).toHaveBeenCalledWith('var-2')
  })
})
