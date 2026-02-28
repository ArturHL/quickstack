import { describe, it, expect, beforeEach } from 'vitest'
import { screen } from '@testing-library/react'
import { renderInRoutes } from '../test-utils/renderWithProviders'
import { useAuthStore } from '../stores/authStore'
import RoleProtectedRoute from '../components/common/RoleProtectedRoute'

function setUser(role: 'OWNER' | 'MANAGER' | 'CASHIER' | 'WAITER') {
  useAuthStore.setState({
    isAuthenticated: true,
    isLoading: false,
    accessToken: 'test-token',
    user: { id: 'user-1', email: 'test@test.com', fullName: 'Test User', role, tenantId: 'tenant-1' },
  })
}

const dashboardRoute = { path: '/dashboard', element: <div>Dashboard</div> }
const loginRoute = { path: '/login', element: <div>Login</div> }

beforeEach(() => {
  useAuthStore.setState({
    isAuthenticated: false,
    isLoading: false,
    accessToken: null,
    user: null,
  })
})

describe('Admin Routes — Role Protection', () => {
  it('/admin/products accessible for MANAGER', async () => {
    setUser('MANAGER')
    renderInRoutes(
      [
        {
          path: '/admin/products', element: (
            <>
              <RoleProtectedRoute minRole="MANAGER" />
              <div>Lista de Productos</div>
            </>
          )
        },
        dashboardRoute,
        loginRoute,
      ],
      { initialRoute: '/admin/products' }
    )
    // MANAGER can access
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument()
  })

  it('/admin/products accessible for OWNER', () => {
    setUser('OWNER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        { path: '/admin/products', element: <div>Lista de Productos</div> },
      ],
      { initialRoute: '/admin/products' }
    )
    expect(screen.getByText('Lista de Productos')).toBeInTheDocument()
  })

  it('RoleProtectedRoute redirects CASHIER for MANAGER-required routes', () => {
    setUser('CASHIER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        {
          path: '/admin/products',
          element: <RoleProtectedRoute minRole="MANAGER" />,
        } as unknown as { path: string; element: React.ReactElement },
      ],
      { initialRoute: '/admin/products' }
    )
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('RoleProtectedRoute redirects MANAGER for OWNER-required routes', () => {
    setUser('MANAGER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        {
          path: '/admin/branches',
          element: <RoleProtectedRoute minRole="OWNER" />,
        } as unknown as { path: string; element: React.ReactElement },
      ],
      { initialRoute: '/admin/branches' }
    )
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })

  it('/admin/branches accessible for OWNER', () => {
    setUser('OWNER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        { path: '/admin/branches', element: <div>Lista de Sucursales</div> },
      ],
      { initialRoute: '/admin/branches' }
    )
    expect(screen.getByText('Lista de Sucursales')).toBeInTheDocument()
  })

  it('/admin/customers accessible for CASHIER', () => {
    setUser('CASHIER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        { path: '/admin/customers', element: <div>Lista de Clientes</div> },
      ],
      { initialRoute: '/admin/customers' }
    )
    expect(screen.getByText('Lista de Clientes')).toBeInTheDocument()
  })

  it('RoleProtectedRoute redirects unauthenticated user to login', () => {
    // user is null (cleared in beforeEach)
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        {
          path: '/admin/products',
          element: <RoleProtectedRoute minRole="MANAGER" />,
        } as unknown as { path: string; element: React.ReactElement },
      ],
      { initialRoute: '/admin/products' }
    )
    expect(screen.getByText('Login')).toBeInTheDocument()
  })

  it('WAITER is redirected from CASHIER+ routes', () => {
    setUser('WAITER')
    renderInRoutes(
      [
        dashboardRoute,
        loginRoute,
        {
          path: '/admin/customers',
          element: <RoleProtectedRoute minRole="CASHIER" />,
        } as unknown as { path: string; element: React.ReactElement },
      ],
      { initialRoute: '/admin/customers' }
    )
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
  })
})
