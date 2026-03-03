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

  it('converts SKU input to uppercase automatically', async () => {
    renderWithProviders(<ProductForm />)

    const skuInput = screen.getByRole('textbox', { name: /sku/i })
    await userEvent.type(skuInput, 'cafe-001')

    expect(skuInput).toHaveValue('CAFE-001')
  })

  it('rejects special characters in SKU', async () => {
    renderWithProviders(<ProductForm />)

    const skuInput = screen.getByRole('textbox', { name: /sku/i })
    await userEvent.type(skuInput, 'café@#!')

    // Only valid chars remain
    expect((skuInput as HTMLInputElement).value).toMatch(/^[A-Z0-9_-]*$/)
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

describe('ProductForm — edit mode VARIANT', () => {
  it('loads existing variants for a VARIANT product', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre producto/i })).toHaveValue('Café con Leche')
    )

    await waitFor(() => {
      const variantInputs = screen.getAllByRole('textbox', { name: /nombre variante/i })
      expect(variantInputs.length).toBe(2)
      expect(variantInputs[0]).toHaveValue('Chico')
      expect(variantInputs[1]).toHaveValue('Grande')
    })
  })

  it('shows existing variant prices', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() => {
      const priceInputs = screen.getAllByRole('spinbutton', { name: /precio variante/i })
      expect(priceInputs[0]).toHaveValue(35)
      expect(priceInputs[1]).toHaveValue(45)
    })
  })

  it('can edit an existing variant name', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() => {
      const variantInputs = screen.getAllByRole('textbox', { name: /nombre variante/i })
      expect(variantInputs[0]).toHaveValue('Chico')
    })

    const variantInputs = screen.getAllByRole('textbox', { name: /nombre variante/i })
    await userEvent.clear(variantInputs[0])
    await userEvent.type(variantInputs[0], 'Pequeño')

    expect(variantInputs[0]).toHaveValue('Pequeño')
  })

  it('can add a new variant in edit mode', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() => {
      expect(screen.getAllByRole('textbox', { name: /nombre variante/i }).length).toBe(2)
    })

    await userEvent.click(screen.getByRole('button', { name: /agregar variante/i }))

    expect(screen.getAllByRole('textbox', { name: /nombre variante/i }).length).toBe(3)
  })

  it('can remove an existing variant in edit mode', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() => {
      expect(screen.getAllByRole('textbox', { name: /nombre variante/i }).length).toBe(2)
    })

    const deleteButtons = screen.getAllByRole('button', { name: /eliminar variante/i })
    await userEvent.click(deleteButtons[0])

    expect(screen.getAllByRole('textbox', { name: /nombre variante/i }).length).toBe(1)
  })

  it('type selector is disabled in edit mode', async () => {
    renderWithProviders(<ProductForm productId="prod-3" />)

    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre producto/i })).toHaveValue('Café con Leche')
    )

    const typeCombobox = screen.getByRole('combobox', { name: /tipo de producto/i })
    expect(typeCombobox).toHaveAttribute('aria-disabled', 'true')
  })
})
