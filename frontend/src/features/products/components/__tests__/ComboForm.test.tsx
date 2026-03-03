import { describe, it, expect, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ComboForm from '../ComboForm'
import type { ComboResponse } from '../../types/Product'

const mockCombo: ComboResponse = {
  id: 'combo-1',
  tenantId: 'tenant-1',
  name: 'Combo Desayuno',
  description: 'Café + Sandwich',
  imageUrl: null,
  price: 105,
  sortOrder: 1,
  isActive: true,
  items: [
    { productId: 'prod-1', productName: 'Café Americano', quantity: 1 },
    { productId: 'prod-2', productName: 'Sandwich Club', quantity: 1 },
  ],
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
}

describe('ComboForm — create mode', () => {
  it('renders with "Nuevo Combo" title', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('heading', { name: /nuevo combo/i })).toBeInTheDocument()
  })

  it('shows name, description and price fields', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('textbox', { name: /nombre combo/i })).toBeInTheDocument()
    expect(screen.getByRole('spinbutton', { name: /precio combo/i })).toBeInTheDocument()
  })

  it('shows validation error when name is empty', async () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })

  it('shows validation error when no items added', async () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.type(screen.getByRole('textbox', { name: /nombre combo/i }), 'Mi Combo')
    await userEvent.type(screen.getByRole('spinbutton', { name: /precio combo/i }), '50')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/agrega al menos un producto/i)).toBeInTheDocument()
  })

  it('shows product selector and add button', async () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )

    expect(screen.getByRole('button', { name: /agregar producto al combo/i })).toBeInTheDocument()
  })

  it('calls onClose when Cancelar is clicked', async () => {
    const onClose = vi.fn()
    renderWithProviders(
      <ComboForm open onClose={onClose} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(onClose).toHaveBeenCalled()
  })
})

describe('ComboForm — edit mode', () => {
  it('renders with "Editar Combo" title', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCombo} />
    )
    expect(screen.getByRole('heading', { name: /editar combo/i })).toBeInTheDocument()
  })

  it('pre-populates name and price', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCombo} />
    )
    expect(screen.getByRole('textbox', { name: /nombre combo/i })).toHaveValue('Combo Desayuno')
    expect(screen.getByRole('spinbutton', { name: /precio combo/i })).toHaveValue(105)
  })

  it('shows existing items in the table', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCombo} />
    )
    expect(screen.getByText('Café Americano')).toBeInTheDocument()
    expect(screen.getByText('Sandwich Club')).toBeInTheDocument()
  })

  it('shows "Guardar" button in edit mode', () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCombo} />
    )
    expect(screen.getByRole('button', { name: /^guardar$/i })).toBeInTheDocument()
  })

  it('can remove an existing item', async () => {
    renderWithProviders(
      <ComboForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCombo} />
    )

    expect(screen.getByText('Café Americano')).toBeInTheDocument()

    await userEvent.click(
      screen.getByRole('button', { name: /quitar Café Americano del combo/i })
    )

    expect(screen.queryByText('Café Americano')).not.toBeInTheDocument()
  })
})
