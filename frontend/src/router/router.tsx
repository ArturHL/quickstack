import { createBrowserRouter, Navigate } from 'react-router-dom'
import LoginPage from '../features/auth/LoginPage'
import RegisterPage from '../features/auth/RegisterPage'
import ForgotPasswordPage from '../features/auth/ForgotPasswordPage'
import ResetPasswordPage from '../features/auth/ResetPasswordPage'
import DashboardPage from '../pages/DashboardPage'
import ProtectedRoute from '../components/common/ProtectedRoute'
import RoleBasedRedirect from '../components/common/RoleBasedRedirect'
import AppLayout from '../components/layout/AppLayout'
import WaiterLayout from '../components/layout/WaiterLayout'
import CashierLayout from '../components/layout/CashierLayout'
import KitchenLayout from '../components/layout/KitchenLayout'
import WaiterTables from '../pages/waiter/WaiterTables'
import CashierPos from '../pages/cashier/CashierPos'
import KitchenBoard from '../pages/kitchen/KitchenBoard'
import { posRoutes } from './posRoutes'
import { adminRoutes } from './adminRoutes'
import { orderRoutes } from './orderRoutes'


export const router = createBrowserRouter(
  [
    {
      path: '/',
      element: <RoleBasedRedirect />,
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
          path: '/waiter',
          element: <WaiterLayout />,
          children: [
            { index: true, element: <Navigate to="tables" replace /> },
            { path: 'tables', element: <WaiterTables /> },
          ],
        },
        {
          path: '/cashier',
          element: <CashierLayout />,
          children: [
            { index: true, element: <Navigate to="pos" replace /> },
            { path: 'pos', element: <CashierPos /> },
          ],
        },
        {
          path: '/kitchen',
          element: <KitchenLayout />,
          children: [
            { index: true, element: <Navigate to="board" replace /> },
            { path: 'board', element: <KitchenBoard /> },
          ],
        },
        {
          path: '/admin',
          element: <AppLayout />,
          children: [
            {
              path: 'dashboard',
              element: <DashboardPage />,
            },
            ...posRoutes,
            ...adminRoutes,
            ...orderRoutes,
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
