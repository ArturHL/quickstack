import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { authApi } from '../../services/authApi'
import LoadingSpinner from './LoadingSpinner'

const ProtectedRoute = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isLoading = useAuthStore((s) => s.isLoading)
  const setAuth = useAuthStore((s) => s.setAuth)
  const setLoading = useAuthStore((s) => s.setLoading)
  const location = useLocation()

  useEffect(() => {
    if (isAuthenticated) return
    setLoading(true)
    authApi
      .refreshToken()
      .then((data) => {
        setAuth(data.accessToken, data.user)
      })
      .catch(() => {
        setLoading(false)
      })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (isLoading) return <LoadingSpinner />

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <Outlet />
}

export default ProtectedRoute
