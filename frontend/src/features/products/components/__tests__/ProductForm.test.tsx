import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ProductForm from '../ProductForm'

beforeEach(() => {
  localStorage.clear()
})

describe('ProductForm — create mode', () => {
  it('renders create form with correct title', async () => {
    renderWithProviders(<ProductForm />)
    expect(screen.getByText('Nuevo Producto')).toBeInTheDocument()
  })

  it('shows name field', () => {
    renderWithProviders(<ProductForm />)
    expect(screen.getByRole('textbox', { name: /nombre producto/i })).toBeInTheDocument()
  })

  it('validates name is required', async () => {
    renderWithProviders(<ProductForm />)

    const priceInput = screen.getByRole('spinbutton', { name: /precio base/i })
    await userEvent.type(priceInput, '35')
    await userEvent.click(screen.getByRole('button', { name: /crear producto/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })

  it('validates price must be non-negative', async () => {
    renderWithProviders(<ProductForm />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre producto/i }), 'Café')
    const priceInput = screen.getByRole('spinbutton', { name: /precio base/i })
    await userEvent.type(priceInput, '-5')
    await userEvent.click(screen.getByRole('button', { name: /crear producto/i }))

    expect(screen.getByText(/precio debe ser mayor/i)).toBeInTheDocument()
  })

  it('shows variant fields when type is VARIANT', async () => {
    renderWithProviders(<ProductForm />)

    const typeSelect = screen.getByRole('combobox', { name: /tipo de producto/i })
    await userEvent.click(typeSelect)
    await userEvent.click(screen.getByText('Con variantes'))

    expect(screen.getByText('Variantes')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /agregar variante/i })).toBeInTheDocument()
  })

  it('can add a variant', async () => {
    renderWithProviders(<ProductForm />)

    const typeSelect = screen.getByRole('combobox', { name: /tipo de producto/i })
    await userEvent.click(typeSelect)
    await userEvent.click(screen.getByText('Con variantes'))

    await userEvent.click(screen.getByRole('button', { name: /agregar variante/i }))

    const variantNameInputs = screen.getAllByRole('textbox', { name: /nombre variante/i })
    expect(variantNameInputs.length).toBe(2)
  })

  it('submits create form with valid data', async () => {
    renderWithProviders(<ProductForm />)

    await userEvent.type(screen.getByRole('textbox', { name: /nombre producto/i }), 'Tamal')
    const priceInput = screen.getByRole('spinbutton', { name: /precio base/i })
    await userEvent.type(priceInput, '25')

    await userEvent.click(screen.getByRole('button', { name: /crear producto/i }))

    // Should navigate away on success (no error shown)
    await waitFor(() =>
      expect(screen.queryByText(/el nombre es requerido/i)).not.toBeInTheDocument()
    )
  })

  it('shows cancel button', () => {
    renderWithProviders(<ProductForm />)
    expect(screen.getByRole('button', { name: /cancelar/i })).toBeInTheDocument()
  })
})

describe('ProductForm — edit mode', () => {
  it('renders edit form with correct title after loading', async () => {
    renderWithProviders(<ProductForm productId="prod-1" />)

    await waitFor(() =>
      expect(screen.getByText('Editar Producto')).toBeInTheDocument()
    )
  })

  it('pre-populates name field with existing data', async () => {
    renderWithProviders(<ProductForm productId="prod-1" />)

    await waitFor(() => {
      const nameInput = screen.getByRole('textbox', { name: /nombre producto/i })
      expect(nameInput).toHaveValue('Café Americano')
    })
  })

  it('shows "Guardar cambios" button in edit mode', async () => {
    renderWithProviders(<ProductForm productId="prod-1" />)

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /guardar cambios/i })).toBeInTheDocument()
    )
  })
})
