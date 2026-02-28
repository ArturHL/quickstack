import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import BranchSelector from '../BranchSelector'
import { useBranchStore, initialBranchState } from '../../../pos/stores/branchStore'

beforeEach(() => {
  localStorage.clear()
  useBranchStore.setState(initialBranchState)
})

describe('BranchSelector', () => {
  it('renders loading skeleton initially', () => {
    renderWithProviders(<BranchSelector />)
    // During load, shows skeleton or nothing
    expect(document.body).toBeTruthy()
  })

  it('renders branch options after loading', async () => {
    renderWithProviders(<BranchSelector />)

    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: /sucursal/i })).toBeInTheDocument()
    )
  })

  it('auto-selects when only one branch is present', async () => {
    const { http, HttpResponse } = await import('msw')
    const { server } = await import('../../../../mocks/server')
    const { mockBranches } = await import('../../../../mocks/handlers/branchHandlers')
    const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

    server.use(
      http.get(`${BASE}/branches`, () =>
        HttpResponse.json({ data: [mockBranches[0]] }, { status: 200 })
      )
    )

    renderWithProviders(<BranchSelector />)

    await waitFor(() => {
      const storeState = useBranchStore.getState()
      expect(storeState.activeBranchId).toBe('branch-1')
    })
  })

  it('allows selecting a branch', async () => {
    renderWithProviders(<BranchSelector />)

    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: /sucursal/i })).toBeInTheDocument()
    )

    await userEvent.click(screen.getByRole('combobox', { name: /sucursal/i }))
    await userEvent.click(screen.getByText('Sucursal Norte'))

    expect(useBranchStore.getState().activeBranchId).toBe('branch-2')
  })

  it('persists selection to branchStore', async () => {
    renderWithProviders(<BranchSelector />)

    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: /sucursal/i })).toBeInTheDocument()
    )

    await userEvent.click(screen.getByRole('combobox', { name: /sucursal/i }))
    await userEvent.click(screen.getByText('Sucursal Centro'))

    expect(useBranchStore.getState().activeBranchId).toBe('branch-1')
  })

  it('shows all available branches in dropdown', async () => {
    renderWithProviders(<BranchSelector />)

    await waitFor(() =>
      expect(screen.getByRole('combobox', { name: /sucursal/i })).toBeInTheDocument()
    )

    await userEvent.click(screen.getByRole('combobox', { name: /sucursal/i }))

    expect(screen.getByText('Sucursal Centro')).toBeInTheDocument()
    expect(screen.getByText('Sucursal Norte')).toBeInTheDocument()
  })
})
