import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import TableList from '../TableList'

beforeEach(() => {
  localStorage.clear()
})

describe('TableList', () => {
  it('renders tables for an area', async () => {
    renderWithProviders(<TableList areaId="area-1" />)

    await waitFor(() =>
      expect(screen.getByText(/Mesa 1/)).toBeInTheDocument()
    )
  })

  it('shows status badge for tables', async () => {
    renderWithProviders(<TableList areaId="area-1" />)

    await waitFor(() => expect(screen.getByText(/Mesa 1/)).toBeInTheDocument())

    expect(screen.getByText('Disponible')).toBeInTheDocument()
    expect(screen.getByText('Ocupada')).toBeInTheDocument()
  })

  it('shows "Nueva Mesa" button', async () => {
    renderWithProviders(<TableList areaId="area-1" />)
    await waitFor(() => expect(screen.getByText(/Mesa 1/)).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /nueva mesa/i })).toBeInTheDocument()
  })

  it('opens table form when Nueva Mesa is clicked', async () => {
    renderWithProviders(<TableList areaId="area-1" />)
    await waitFor(() => expect(screen.getByText(/Mesa 1/)).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva mesa/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByRole('spinbutton', { name: /número de mesa/i })).toBeInTheDocument()
  })

  it('creates a table', async () => {
    renderWithProviders(<TableList areaId="area-1" />)
    await waitFor(() => expect(screen.getByText(/Mesa 1/)).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /nueva mesa/i }))
    await userEvent.type(screen.getByRole('spinbutton', { name: /número de mesa/i }), '5')
    await userEvent.type(screen.getByRole('spinbutton', { name: /capacidad/i }), '4')
    await userEvent.click(screen.getByRole('button', { name: /^crear$/i }))

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    )
  })

  it('shows delete confirmation when delete is clicked', async () => {
    renderWithProviders(<TableList areaId="area-1" />)
    await waitFor(() => expect(screen.getByText(/Mesa 1/)).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: /eliminar mesa 1/i }))

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText(/¿Eliminar mesa\?/i)).toBeInTheDocument()
  })
})
