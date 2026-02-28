import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import CustomerList from '../CustomerList'

beforeEach(() => {
  localStorage.clear()
})

describe('CustomerList', () => {
  it('renders customer list after loading', async () => {
    renderWithProviders(<CustomerList />)

    await waitFor(() =>
      expect(screen.getByText('Juan García')).toBeInTheDocument()
    )
    expect(screen.getByText('María López')).toBeInTheDocument()
  })

  it('shows Clientes heading', async () => {
    renderWithProviders(<CustomerList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    expect(screen.getByRole('heading', { name: /clientes/i })).toBeInTheDocument()
  })

  it('shows search input', async () => {
    renderWithProviders(<CustomerList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())
    expect(screen.getByRole('textbox', { name: /buscar cliente/i })).toBeInTheDocument()
  })

  it('filters customers by search', async () => {
    renderWithProviders(<CustomerList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.type(screen.getByRole('textbox', { name: /buscar cliente/i }), 'Juan')

    await waitFor(() =>
      expect(screen.queryByText('María López')).not.toBeInTheDocument()
    )
    expect(screen.getByText('Juan García')).toBeInTheDocument()
  })

  it('opens edit dialog on row click', async () => {
    renderWithProviders(<CustomerList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByText('Juan García'))

    expect(screen.getByText('Editar Cliente')).toBeInTheDocument()
    expect(screen.getByRole('textbox', { name: /nombre cliente/i })).toHaveValue('Juan García')
  })

  it('saves customer edit', async () => {
    renderWithProviders(<CustomerList />)
    await waitFor(() => expect(screen.getByText('Juan García')).toBeInTheDocument())

    await userEvent.click(screen.getByText('Juan García'))

    const nameInput = screen.getByRole('textbox', { name: /nombre cliente/i })
    await userEvent.clear(nameInput)
    await userEvent.type(nameInput, 'Juan García Updated')

    await userEvent.click(screen.getByRole('button', { name: /guardar/i }))

    await waitFor(() =>
      expect(screen.queryByText('Editar Cliente')).not.toBeInTheDocument()
    )
  })
})
