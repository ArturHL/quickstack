import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ModifierGroup from '../ModifierGroup'
import type { MenuModifierGroupItem } from '../../types/Menu'

const multiSelectGroup: MenuModifierGroupItem = {
  id: 'grp-1',
  name: 'Extras',
  minSelections: 0,
  maxSelections: 2,
  isRequired: false,
  modifiers: [
    { id: 'mod-1', name: 'Leche extra', priceAdjustment: 5.0, isDefault: false, sortOrder: 1 },
    { id: 'mod-2', name: 'Azúcar', priceAdjustment: 0, isDefault: false, sortOrder: 2 },
    { id: 'mod-3', name: 'Canela', priceAdjustment: -2.0, isDefault: false, sortOrder: 3 },
  ],
}

const singleSelectGroup: MenuModifierGroupItem = {
  id: 'grp-2',
  name: 'Tamaño de bebida',
  minSelections: 1,
  maxSelections: 1,
  isRequired: true,
  modifiers: [
    { id: 'mod-4', name: 'Chico', priceAdjustment: 0, isDefault: false, sortOrder: 1 },
    { id: 'mod-5', name: 'Grande', priceAdjustment: 10.0, isDefault: false, sortOrder: 2 },
  ],
}

describe('ModifierGroup', () => {
  it('renders group name', () => {
    renderWithProviders(<ModifierGroup group={multiSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    expect(screen.getByText('Extras')).toBeInTheDocument()
  })

  it('shows "Requerido" chip when isRequired=true', () => {
    renderWithProviders(<ModifierGroup group={singleSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    expect(screen.getByText('Requerido')).toBeInTheDocument()
  })

  it('does NOT show "Requerido" chip when isRequired=false', () => {
    renderWithProviders(<ModifierGroup group={multiSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    expect(screen.queryByText('Requerido')).not.toBeInTheDocument()
  })

  it('renders modifiers as checkboxes when maxSelections > 1', () => {
    renderWithProviders(<ModifierGroup group={multiSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    const checkboxes = screen.getAllByRole('checkbox')
    expect(checkboxes).toHaveLength(3)
  })

  it('renders modifiers as radio buttons when maxSelections == 1', () => {
    renderWithProviders(<ModifierGroup group={singleSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    const radios = screen.getAllByRole('radio')
    expect(radios).toHaveLength(2)
  })

  it('calls onChange with added modifier when checkbox is checked', async () => {
    const handleChange = vi.fn()
    renderWithProviders(<ModifierGroup group={multiSelectGroup} selectedIds={[]} onChange={handleChange} />)

    await userEvent.click(screen.getByRole('checkbox', { name: /Leche extra/ }))

    expect(handleChange).toHaveBeenCalledWith(['mod-1'])
  })

  it('calls onChange with single id when radio is selected', async () => {
    const handleChange = vi.fn()
    renderWithProviders(<ModifierGroup group={singleSelectGroup} selectedIds={[]} onChange={handleChange} />)

    await userEvent.click(screen.getByRole('radio', { name: /Grande/ }))

    expect(handleChange).toHaveBeenCalledWith(['mod-5'])
  })

  it('disables unchecked checkboxes when maxSelections is reached', () => {
    // max is 2, already have 2 selected
    renderWithProviders(
      <ModifierGroup group={multiSelectGroup} selectedIds={['mod-1', 'mod-2']} onChange={vi.fn()} />
    )

    // mod-3 (Canela) should be disabled since max=2 is reached
    expect(screen.getByRole('checkbox', { name: /Canela/ })).toBeDisabled()
    // already-selected ones stay enabled
    expect(screen.getByRole('checkbox', { name: /Leche extra/ })).not.toBeDisabled()
  })

  it('shows price adjustment in modifier label', () => {
    renderWithProviders(<ModifierGroup group={multiSelectGroup} selectedIds={[]} onChange={vi.fn()} />)

    expect(screen.getByText(/Leche extra.*\+\$5\.00/)).toBeInTheDocument()
    expect(screen.getByText(/Canela.*-\$2\.00/)).toBeInTheDocument()
  })
})
