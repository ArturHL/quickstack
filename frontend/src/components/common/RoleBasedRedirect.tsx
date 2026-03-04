import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import type { AuthUser } from '../../types/auth'
import LoadingSpinner from './LoadingSpinner'

type Role = AuthUser['role']

const ROLE_ROUTES: Record<Role, string> = {
    WAITER: '/waiter/tables',
    CASHIER: '/cashier/pos',
    KITCHEN: '/kitchen/board',
    MANAGER: '/admin/reports',
    OWNER: '/admin/branches',
}

export default function RoleBasedRedirect() {
    const { user, isLoading, isAuthenticated } = useAuthStore()
    const location = useLocation()

    if (isLoading) return <LoadingSpinner />

    if (!isAuthenticated || !user) {
        return <Navigate to="/login" state={{ from: location }} replace />
    }

    const destination = ROLE_ROUTES[user.role] ?? '/login'
    return <Navigate to={destination} replace />
}
