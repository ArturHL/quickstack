import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor, fireEvent } from '@testing-library/react'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import DailySummaryPage from '../DailySummaryPage'
import { useBranchStore } from '../../../pos/stores/branchStore'

beforeEach(() => {
  localStorage.clear()
  useBranchStore.setState({ activeBranchId: 'branch-1' })
})

describe('DailySummaryPage', () => {
  it('renders page title', () => {
    renderWithProviders(<DailySummaryPage />)
    expect(screen.getByText('Reportes')).toBeInTheDocument()
  })

  it('shows message when no branch is selected', () => {
    useBranchStore.setState({ activeBranchId: null })
    renderWithProviders(<DailySummaryPage />)
    expect(screen.getByText(/selecciona una sucursal/i)).toBeInTheDocument()
  })

  it('shows date selector defaulting to today', () => {
    renderWithProviders(<DailySummaryPage />)
    const today = new Date().toISOString().split('T')[0]
    const dateInput = screen.getByLabelText(/fecha reporte/i)
    expect(dateInput).toHaveValue(today)
  })

  it('shows metric cards after loading', async () => {
    renderWithProviders(<DailySummaryPage />)

    await waitFor(() =>
      expect(screen.getByText('$1250.50')).toBeInTheDocument()
    )
    expect(screen.getByText('18')).toBeInTheDocument()
    expect(screen.getByText('$69.47')).toBeInTheDocument()
  })

  it('shows metric card labels', async () => {
    renderWithProviders(<DailySummaryPage />)

    await waitFor(() => expect(screen.getByText('$1250.50')).toBeInTheDocument())

    expect(screen.getByText('Total ventas')).toBeInTheDocument()
    expect(screen.getByText('Pedidos')).toBeInTheDocument()
    expect(screen.getByText('Ticket promedio')).toBeInTheDocument()
  })

  it('shows top products table', async () => {
    renderWithProviders(<DailySummaryPage />)

    await waitFor(() =>
      expect(screen.getByText('Productos más vendidos')).toBeInTheDocument()
    )
    expect(screen.getByText('Café Americano')).toBeInTheDocument()
    expect(screen.getByText('Sandwich Club')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
  })

  it('shows empty state when date has no sales', async () => {
    renderWithProviders(<DailySummaryPage />)

    await waitFor(() => expect(screen.getByText('$1250.50')).toBeInTheDocument())

    const dateInput = screen.getByLabelText(/fecha reporte/i)
    fireEvent.change(dateInput, { target: { value: '2020-01-01' } })

    await waitFor(() =>
      expect(screen.getByText(/sin ventas registradas/i)).toBeInTheDocument()
    )
  })

  it('shows ranked position numbers in top products', async () => {
    renderWithProviders(<DailySummaryPage />)

    await waitFor(() =>
      expect(screen.getByText('Café Americano')).toBeInTheDocument()
    )

    const rows = screen.getAllByRole('row')
    // Header + 3 products = 4 rows
    expect(rows.length).toBe(4)
  })
})
