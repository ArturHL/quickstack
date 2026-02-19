import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import LoadingSpinner from './LoadingSpinner'

const ProtectedRoute = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isLoading = useAuthStore((s) => s.isLoading)
  const location = useLocation()

  if (isLoading) return <LoadingSpinner />

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}

export default ProtectedRoute
