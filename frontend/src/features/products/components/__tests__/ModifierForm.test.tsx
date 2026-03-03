import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ModifierForm from '../ModifierForm'
import type { ModifierResponse } from '../../types/Product'

const mockModifier: ModifierResponse = {
  id: 'mod-1',
  groupId: 'grp-1',
  tenantId: 'tenant-1',
  name: 'Caliente',
  priceAdjustment: 5,
  isDefault: true,
  sortOrder: 1,
  isActive: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

describe('ModifierForm — create mode', () => {
  it('renders with "Nuevo Modificador" title', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('heading', { name: /nuevo modificador/i })).toBeInTheDocument()
  })

  it('shows name and price fields', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('textbox', { name: /nombre modificador/i })).toBeInTheDocument()
    expect(screen.getByRole('spinbutton', { name: /precio adicional/i })).toBeInTheDocument()
  })

  it('price defaults to 0', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('spinbutton', { name: /precio adicional/i })).toHaveValue(0)
  })

  it('shows validation error when name is empty', async () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })

  it('calls onSubmit with name and priceAdjustment', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={onSubmit} isPending={false} />
    )

    await userEvent.type(screen.getByRole('textbox', { name: /nombre modificador/i }), 'Sin cebolla')
    const priceInput = screen.getByRole('spinbutton', { name: /precio adicional/i })
    await userEvent.clear(priceInput)
    await userEvent.type(priceInput, '5')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Sin cebolla', priceAdjustment: 5 })
  })

  it('calls onClose when Cancelar is clicked', async () => {
    const onClose = vi.fn()
    renderWithProviders(
      <ModifierForm open onClose={onClose} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(onClose).toHaveBeenCalled()
  })
})

describe('ModifierForm — edit mode', () => {
  it('renders with "Editar Modificador" title', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockModifier} />
    )
    expect(screen.getByRole('heading', { name: /editar modificador/i })).toBeInTheDocument()
  })

  it('pre-populates fields with existing data', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockModifier} />
    )
    expect(screen.getByRole('textbox', { name: /nombre modificador/i })).toHaveValue('Caliente')
    expect(screen.getByRole('spinbutton', { name: /precio adicional/i })).toHaveValue(5)
  })

  it('shows "Guardar" button in edit mode', () => {
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockModifier} />
    )
    expect(screen.getByRole('button', { name: /^guardar$/i })).toBeInTheDocument()
  })

  it('calls onSubmit with updated values', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(
      <ModifierForm open onClose={vi.fn()} onSubmit={onSubmit} isPending={false} initial={mockModifier} />
    )

    const nameInput = screen.getByRole('textbox', { name: /nombre modificador/i })
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Tibio')
    await userEvent.click(screen.getByRole('button', { name: /^guardar$/i }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Tibio', priceAdjustment: 5 })
  })
})
