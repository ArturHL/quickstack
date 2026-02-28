import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import type { AuthUser } from '../../types/auth'

type UserRole = AuthUser['role']

const ROLE_RANK: Record<UserRole, number> = {
  WAITER: 0,
  CASHIER: 1,
  MANAGER: 2,
  OWNER: 3,
}

interface RoleProtectedRouteProps {
  minRole: UserRole
}

export default function RoleProtectedRoute({ minRole }: RoleProtectedRouteProps) {
  const user = useAuthStore((s) => s.user)

  if (!user) {
    return <Navigate to="/login" replace />
  }

  const hasAccess = (ROLE_RANK[user.role] ?? -1) >= ROLE_RANK[minRole]

  if (!hasAccess) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
