import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import ComboList from '../ComboList'

beforeEach(() => {
  localStorage.clear()
})

describe('ComboList', () => {
  it('renders combos after loading', async () => {
    renderWithProviders(<ComboList />)

    await waitFor(() =>
      expect(screen.getByText('Combo Desayuno')).toBeInTheDocument()
    )
    expect(screen.getByText('Combo Doble Café')).toBeInTheDocument()
  })

  it('shows "Nuevo Combo" button', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    expect(screen.getByRole('button', { name: /nuevo combo/i })).toBeInTheDocument()
  })

  it('shows price formatted with $', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    expect(screen.getByText('$105.00')).toBeInTheDocument()
  })

  it('shows combo components in table', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    expect(screen.getByText(/Café Americano ×1/)).toBeInTheDocument()
  })

  it('shows active chip', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    const chips = screen.getAllByText('Activo')
    expect(chips.length).toBeGreaterThan(0)
  })

  it('opens create form when Nuevo Combo is clicked', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nuevo combo/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre combo/i })).toBeInTheDocument()
  })

  it('opens edit form with existing data', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar Combo Desayuno/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre combo/i })).toHaveValue('Combo Desayuno')
    )
  })

  it('shows delete confirmation when delete is clicked', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Combo Desayuno/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar combo\?/i)).toBeInTheDocument()
  })

  it('confirms delete and closes dialog', async () => {
    renderWithProviders(<ComboList />)
    await waitFor(() => expect(screen.getByText('Combo Desayuno')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Combo Desayuno/i }))
    await userEvent.click(screen.getByRole('button', { name: /^eliminar$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })
})
