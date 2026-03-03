import { describe, it, expect, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import CategoryForm from '../CategoryForm'
import type { CategoryResponse } from '../../types/Product'

const mockCategory: CategoryResponse = {
  id: 'cat-1',
  tenantId: 'tenant-1',
  name: 'Bebidas',
  description: 'Todas las bebidas',
  imageUrl: null,
  sortOrder: 1,
  isActive: true,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
  createdBy: 'admin',
  updatedBy: 'admin',
}

describe('CategoryForm — create mode', () => {
  it('renders with "Nueva Categoría" title', () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('heading', { name: /nueva categoría/i })).toBeInTheDocument()
  })

  it('shows name and description fields', () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )
    expect(screen.getByRole('textbox', { name: /nombre categoría/i })).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /descripción categoría/i })).toBeInTheDocument()
  })

  it('shows validation error when name is empty', async () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })

  it('calls onSubmit with name and description', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={onSubmit} isPending={false} />
    )

    await userEvent.type(screen.getByRole('textbox', { name: /nombre categoría/i }), 'Postres')
    await userEvent.type(screen.getByRole('textbox', { name: /descripción categoría/i }), 'Dulces y postres')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Postres', description: 'Dulces y postres' })
  })

  it('calls onClose when Cancelar is clicked', async () => {
    const onClose = vi.fn()
    renderWithProviders(
      <CategoryForm open onClose={onClose} onSubmit={vi.fn()} isPending={false} />
    )

    await userEvent.click(screen.getByRole('button', { name: /cancelar/i }))

    expect(onClose).toHaveBeenCalled()
  })
})

describe('CategoryForm — edit mode', () => {
  it('renders with "Editar Categoría" title', () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCategory} />
    )
    expect(screen.getByRole('heading', { name: /editar categoría/i })).toBeInTheDocument()
  })

  it('pre-populates fields with existing data', () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCategory} />
    )
    expect(screen.getByRole('textbox', { name: /nombre categoría/i })).toHaveValue('Bebidas')
    expect(screen.getByRole('textbox', { name: /descripción categoría/i })).toHaveValue('Todas las bebidas')
  })

  it('shows "Guardar" button in edit mode', () => {
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={vi.fn()} isPending={false} initial={mockCategory} />
    )
    expect(screen.getByRole('button', { name: /^guardar$/i })).toBeInTheDocument()
  })

  it('calls onSubmit with updated values', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(
      <CategoryForm open onClose={vi.fn()} onSubmit={onSubmit} isPending={false} initial={mockCategory} />
    )

    const nameInput = screen.getByRole('textbox', { name: /nombre categoría/i })
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Bebidas Calientes')
    await userEvent.click(screen.getByRole('button', { name: /^guardar$/i }))

    expect(onSubmit).toHaveBeenCalledWith({ name: 'Bebidas Calientes', description: 'Todas las bebidas' })
  })
})
