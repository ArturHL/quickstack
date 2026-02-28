import RoleProtectedRoute from '../components/common/RoleProtectedRoute'
import ProductListPage from '../features/products/pages/ProductListPage'
import ProductFormPage from '../features/products/pages/ProductFormPage'
import BranchListPage from '../features/branches/pages/BranchListPage'
import CustomerListPage from '../features/customers/pages/CustomerListPage'

export const adminRoutes = [
  {
    element: <RoleProtectedRoute minRole="MANAGER" />,
    children: [
      { path: '/admin/products', element: <ProductListPage /> },
      { path: '/admin/products/new', element: <ProductFormPage /> },
      { path: '/admin/products/:id/edit', element: <ProductFormPage /> },
    ],
  },
  {
    element: <RoleProtectedRoute minRole="OWNER" />,
    children: [
      { path: '/admin/branches', element: <BranchListPage /> },
    ],
  },
  {
    element: <RoleProtectedRoute minRole="CASHIER" />,
    children: [
      { path: '/admin/customers', element: <CustomerListPage /> },
    ],
  },
]
