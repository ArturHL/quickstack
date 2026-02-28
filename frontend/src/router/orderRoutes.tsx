import RoleProtectedRoute from '../components/common/RoleProtectedRoute'
import OrderListPage from '../features/orders/pages/OrderListPage'
import OrderDetailPage from '../features/orders/pages/OrderDetailPage'

export const orderRoutes = [
    {
        element: <RoleProtectedRoute minRole="CASHIER" />,
        children: [
            { path: '/orders', element: <OrderListPage /> },
            { path: '/orders/:id', element: <OrderDetailPage /> },
        ],
    },
]
