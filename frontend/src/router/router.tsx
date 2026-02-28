import { createBrowserRouter, Navigate } from 'react-router-dom'
import LoginPage from '../features/auth/LoginPage'
import RegisterPage from '../features/auth/RegisterPage'
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage'
import ResetPasswordPage from '../features/auth/ResetPasswordPage'
import DashboardPage from '../pages/DashboardPage'
import ProtectedRoute from '../components/common/ProtectedRoute'
import AppLayout from '../components/layout/AppLayout'
import { posRoutes } from './posRoutes'

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
          element: <AppLayout />,
          children: [
            {
              path: '/dashboard',
              element: <DashboardPage />,
            },
            ...posRoutes,
          ],
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
