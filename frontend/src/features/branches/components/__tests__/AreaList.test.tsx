import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import AreaList from '../AreaList'

beforeEach(() => {
  localStorage.clear()
})

describe('AreaList', () => {
  it('renders areas for a branch', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)

    await waitFor(() =>
      expect(screen.getByText('Terraza')).toBeInTheDocument()
    )
  })

  it('shows "Nueva Área" button', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /nueva área/i })).toBeInTheDocument()
  })

  it('opens area form when Nueva Área is clicked', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva área/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre área/i })).toBeInTheDocument()
  })

  it('creates an area', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva área/i }))
    await userEvent.type(screen.getByRole('textbox', { name: /nombre área/i }), 'Patio')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('opens edit form with existing data', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /editar área Terraza/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await waitFor(() =>
      expect(screen.getByRole('textbox', { name: /nombre área/i })).toHaveValue('Terraza')
    )
  })

  it('shows delete confirmation when delete is clicked', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar área Terraza/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar área\?/i)).toBeInTheDocument()
  })

  it('validates name required in area form', async () => {
    renderWithProviders(<AreaList branchId="branch-1" />)
    await waitFor(() => expect(screen.getByText('Terraza')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva área/i }))
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    expect(screen.getByText(/el nombre es requerido/i)).toBeInTheDocument()
  })
})
