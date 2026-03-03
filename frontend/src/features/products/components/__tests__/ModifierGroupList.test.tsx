import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ModifierGroupList from '../ModifierGroupList'

const PRODUCT_ID = 'prod-1'

beforeEach(() => {
  localStorage.clear()
})

describe('ModifierGroupList', () => {
  it('renders modifier groups after loading', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)

    await waitFor(() =>
      expect(screen.getByText('Temperatura')).toBeInTheDocument()
    )
  })

  it('shows Única chip for SINGLE selection group', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)

    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())
    expect(screen.getByText('Única')).toBeInTheDocument()
  })

  it('shows Requerido chip for required group', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)

    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())
    expect(screen.getByText('Requerido')).toBeInTheDocument()
  })

  it('shows "Agregar Grupo" button', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    expect(screen.getByRole('button', { name: /agregar grupo/i })).toBeInTheDocument()
  })

  it('opens create form when Agregar Grupo is clicked', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /agregar grupo/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre grupo/i })).toBeInTheDocument()
  })

  it('creates a modifier group', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /agregar grupo/i }))
    await userEvent.type(screen.getByRole('textbox', { name: /nombre grupo/i }), 'Tamaño')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('opens edit form for existing group', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar grupo Temperatura/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre grupo/i })).toHaveValue('Temperatura')
    )
  })

  it('shows delete confirmation for group', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar grupo Temperatura/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar el grupo/i)).toBeInTheDocument()
  })

  it('shows modifiers inside accordion when expanded', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    // Expand the accordion
    await userEvent.click(screen.getByText('Temperatura'))

    await waitFor(() =>
      expect(screen.getByText('Caliente')).toBeInTheDocument()
    )
    expect(screen.getByText('Frío')).toBeInTheDocument()
  })

  it('shows empty state when product has no groups', async () => {
    renderWithProviders(<ModifierGroupList productId="prod-2" />)

    await waitFor(() =>
      expect(screen.getByText(/sin grupos de modificadores/i)).toBeInTheDocument()
    )
  })

  it('validates name is required in group form', async () => {
    renderWithProviders(<ModifierGroupList productId={PRODUCT_ID} />)
    await waitFor(() => expect(screen.getByText('Temperatura')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /agregar grupo/i }))
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })
})
