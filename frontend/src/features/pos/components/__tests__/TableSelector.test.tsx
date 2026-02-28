import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import TableSelector from '../TableSelector'
import { useBranchStore, initialBranchState } from '../../stores/branchStore'
import { useCartStore, initialCartState } from '../../stores/cartStore'

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  useBranchStore.setState(initialBranchState)
  useCartStore.setState(initialCartState)
})

describe('TableSelector', () => {
  it('shows loading spinner while fetching areas', () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    expect(screen.getByRole('status', { name: /cargando áreas/i })).toBeInTheDocument()
  })

  it('renders area tabs after loading', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('tab', { name: /terraza/i })).toBeInTheDocument())
    expect(screen.getByRole('tab', { name: /interior/i })).toBeInTheDocument()
  })

  it('renders tables for the first area', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('button', { name: /mesa 1/i })).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /mesa 2/i })).toBeInTheDocument()
  })

  it('shows AVAILABLE chip for available tables', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByText('AVAILABLE')).toBeInTheDocument())
  })

  it('disables OCCUPIED table', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('button', { name: /mesa 2/i })).toBeInTheDocument())

    expect(screen.getByRole('button', { name: /mesa 2/i })).toBeDisabled()
  })

  it('AVAILABLE table is not disabled', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('button', { name: /mesa 1/i })).toBeInTheDocument())

    expect(screen.getByRole('button', { name: /mesa 1/i })).not.toBeDisabled()
  })

  it('saves tableId in cartStore when AVAILABLE table is clicked', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    useCartStore.setState({ ...initialCartState, serviceType: 'DINE_IN' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('button', { name: /mesa 1/i })).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: /mesa 1/i }))

    expect(useCartStore.getState().tableId).toBe('table-1')
  })

  it('switches to second area tab and loads its tables', async () => {
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderWithProviders(<TableSelector />)

    await waitFor(() => expect(screen.getByRole('tab', { name: /interior/i })).toBeInTheDocument())
    await userEvent.click(screen.getByRole('tab', { name: /interior/i }))

    await waitFor(() => expect(screen.getByRole('button', { name: /mesa 3/i })).toBeInTheDocument())
    expect(screen.getByText('VIP')).toBeInTheDocument()
  })
})
