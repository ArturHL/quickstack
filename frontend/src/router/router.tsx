import { createBrowserRouter, Navigate } from 'react-router-dom'
import LoginPage from '../features/auth/LoginPage'
import RegisterPage from '../features/auth/RegisterPage'
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage'
import ResetPasswordPage from '../features/auth/ResetPasswordPage'
import DashboardPage from '../pages/DashboardPage'
import ProtectedRoute from '../components/common/ProtectedRoute'

export const router = createBrowserRouter(
  [
    {
      path: '/',
      element: <Navigate to="/dashboard" replace />,
    },
    {
      path: '/login',
      element: <LoginPage />,
    },
    {
      path: '/register',
      element: <RegisterPage />,
    },
    {
      path: '/forgot-password',
      element: <ForgotPasswordPage />,
    },
    {
      path: '/reset-password',
      element: <ResetPasswordPage />,
    },
    {
      element: <ProtectedRoute />,
      children: [
        {
          path: '/dashboard',
          element: <DashboardPage />,
        },
      ],
    },
    {
      path: '*',
      element: <div>404 Not Found</div>,
    },
  ],
  {
    future: {
      v7_relativeSplatPath: true,
    },
  }
)
