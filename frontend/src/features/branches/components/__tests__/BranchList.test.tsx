import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import BranchList from '../BranchList'

beforeEach(() => {
  localStorage.clear()
})

describe('BranchList', () => {
  it('renders branch list after loading', async () => {
    renderWithProviders(<BranchList />)

    await waitFor(() =>
      expect(screen.getByText('Sucursal Centro')).toBeInTheDocument()
    )
    expect(screen.getByText('Sucursal Norte')).toBeInTheDocument()
  })

  it('shows "Nueva Sucursal" button', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /nueva sucursal/i })).toBeInTheDocument()
  })

  it('opens create form when Nueva Sucursal is clicked', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva sucursal/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre sucursal/i })).toBeInTheDocument()
  })

  it('creates a branch', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva sucursal/i }))
    await userEvent.type(screen.getByRole('textbox', { name: /nombre sucursal/i }), 'Sucursal Sur')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('opens edit form with existing data', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar Sucursal Centro/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre sucursal/i })).toHaveValue('Sucursal Centro')
    )
  })

  it('shows delete confirmation when delete is clicked', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Sucursal Centro/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar sucursal\?/i)).toBeInTheDocument()
  })

  it('confirms delete and closes dialog', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar Sucursal Centro/i }))
    await userEvent.click(screen.getByRole('button', { name: /^eliminar$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('validates name is required in form', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva sucursal/i }))
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })

  it('shows city and phone in table', async () => {
    renderWithProviders(<BranchList />)
    await waitFor(() => expect(screen.getByText('Sucursal Centro')).toBeInTheDocument())

    expect(screen.getAllByText('CDMX').length).toBeGreaterThan(0)
    expect(screen.getByText('5551234567')).toBeInTheDocument()
  })
})
