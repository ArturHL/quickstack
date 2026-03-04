import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../../../test-utils/renderWithProviders'
import Sidebar from '../Sidebar'
import { useAuthStore } from '../../../stores/authStore'
import { useBranchStore } from '../../../features/pos/stores/branchStore'
import type { AuthUser } from '../../../types/auth'

const ownerUser: AuthUser = {
  id: 'user-owner',
  email: 'owner@test.com',
  fullName: 'El Dueño',
  role: 'OWNER',
  tenantId: 'tenant-1',
}

const cashierUser: AuthUser = {
  id: 'user-cashier',
  email: 'cajero@test.com',
  fullName: 'Cajero',
  role: 'CASHIER',
  tenantId: 'tenant-1',
}

const managerUser: AuthUser = {
  id: 'user-manager',
  email: 'manager@test.com',
  fullName: 'Manager',
  role: 'MANAGER',
  tenantId: 'tenant-1',
}

function renderSidebar() {
  return renderWithProviders(<Sidebar mobileOpen={false} onMobileClose={() => {}} />)
}

beforeEach(() => {
  localStorage.clear()
  useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false, isLoading: false })
  useBranchStore.setState({ activeBranchId: null })
})

// Both permanent and temporary drawers render in jsdom (no CSS media queries),
// so each text appears twice. We use getAllByText(...)[0] or queryAllByText.
function q(text: string) {
  return screen.queryAllByText(text).length > 0
}

describe('Sidebar — Owner Global View', () => {
  it('shows "Vista Global" context label when OWNER has no branch selected', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    renderSidebar()

    expect(screen.getAllByText('Vista Global').length).toBeGreaterThan(0)
  })

  it('shows "Gestión Global" section and Owner nav items', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    renderSidebar()

    expect(q('Gestión Global')).toBe(true)
    expect(q('Sucursales')).toBe(true)
    expect(q('Usuarios')).toBe(true)
  })

  it('hides branch-specific nav items in global view', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    renderSidebar()

    expect(q('Dashboard')).toBe(false)
    expect(q('Pedidos')).toBe(false)
    expect(q('Catálogo')).toBe(false)
    expect(q('Clientes')).toBe(false)
  })

  it('shows "Reportes globales" as disabled with "Próximamente"', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    renderSidebar()

    expect(q('Reportes globales')).toBe(true)
    expect(q('Próximamente')).toBe(true)
  })
})

describe('Sidebar — Branch View (OWNER with branch selected)', () => {
  it('shows branch name as context label', async () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderSidebar()

    await waitFor(() =>
      expect(screen.getAllByText('Sucursal Centro').length).toBeGreaterThan(0)
    )
  })

  it('shows branch view nav items (Catálogo, Pedidos)', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderSidebar()

    expect(q('Catálogo')).toBe(true)
    expect(q('Pedidos')).toBe(true)
    expect(q('Catálogo')).toBe(true)
  })

  it('shows Categorías, Productos, Combos for OWNER in branch view', () => {
    useAuthStore.setState({ user: ownerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderSidebar()

    expect(q('Categorías')).toBe(true)
    expect(q('Productos')).toBe(true)
    expect(q('Combos')).toBe(true)
  })
})

describe('Sidebar — Branch View (CASHIER)', () => {
  it('shows Catálogo, Pedidos, Clientes but NOT Categorías', () => {
    useAuthStore.setState({ user: cashierUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderSidebar()

    expect(q('Catálogo')).toBe(true)
    expect(q('Pedidos')).toBe(true)
    expect(q('Clientes')).toBe(true)
    expect(q('Categorías')).toBe(false)
    expect(q('Productos')).toBe(false)
  })
})

describe('Sidebar — Branch View (MANAGER)', () => {
  it('shows Reporte del día, Categorías, Productos, Combos, Clientes', () => {
    useAuthStore.setState({ user: managerUser, isAuthenticated: true, isLoading: false, accessToken: 'tok' })
    useBranchStore.setState({ activeBranchId: 'branch-1' })
    renderSidebar()

    expect(q('Reporte del día')).toBe(true)
    expect(q('Categorías')).toBe(true)
    expect(q('Productos')).toBe(true)
    expect(q('Combos')).toBe(true)
    expect(q('Clientes')).toBe(true)
  })
})
