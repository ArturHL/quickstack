import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import CategoryList from '../CategoryList'

beforeEach(() => {
  localStorage.clear()
})

describe('CategoryList', () => {
  it('renders categories after loading', async () => {
    renderWithProviders(<CategoryList />)

    await waitFor(() =>
      expect(screen.getByText('Bebidas')).toBeInTheDocument()
    )
    expect(screen.getByText('Comida')).toBeInTheDocument()
  })

  it('shows "Nueva Categoría" button', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    expect(screen.getByRole('button', { name: /nueva categoría/i })).toBeInTheDocument()
  })

  it('opens create form when Nueva Categoría is clicked', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva categoría/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre categoría/i })).toBeInTheDocument()
  })

  it('creates a category', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva categoría/i }))
    await userEvent.type(screen.getByRole('textbox', { name: /nombre categoría/i }), 'Postres')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('opens edit form with existing data', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar Bebidas/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre categoría/i })).toHaveValue('Bebidas')
    )
  })

  it('shows delete confirmation when delete is clicked', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Bebidas/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar categoría\?/i)).toBeInTheDocument()
  })

  it('confirms delete and closes dialog', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Bebidas/i }))
    await userEvent.click(screen.getByRole('button', { name: /^eliminar$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('shows Estado column with chip', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    const chips = screen.getAllByText('Activa')
    expect(chips.length).toBeGreaterThan(0)
  })

  it('validates name is required in create form', async () => {
    renderWithProviders(<CategoryList />)
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva categoría/i }))
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })
})
