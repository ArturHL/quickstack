import RoleProtectedRoute from '../components/common/RoleProtectedRoute'
import ProductListPage from '../features/products/pages/ProductListPage'
import ProductFormPage from '../features/products/pages/ProductFormPage'
import CategoryListPage from '../features/products/pages/CategoryListPage'
import ComboListPage from '../features/products/pages/ComboListPage'
import BranchListPage from '../features/branches/pages/BranchListPage'
import CustomerListPage from '../features/customers/pages/CustomerListPage'
import DailySummaryPage from '../features/reports/pages/DailySummaryPage'

export const adminRoutes = [
  {
    element: <RoleProtectedRoute minRole="MANAGER" />,
    children: [
      { path: 'categories', element: <CategoryListPage /> },
      { path: 'products', element: <ProductListPage /> },
      { path: 'products/new', element: <ProductFormPage /> },
      { path: 'products/:id/edit', element: <ProductFormPage /> },
      { path: 'combos', element: <ComboListPage /> },
      { path: 'reports', element: <DailySummaryPage /> },
    ],
  },
  {
    element: <RoleProtectedRoute minRole="OWNER" />,
    children: [
      { path: 'branches', element: <BranchListPage /> },
    ],
  },
  {
    element: <RoleProtectedRoute minRole="CASHIER" />,
    children: [
      { path: 'customers', element: <CustomerListPage /> },
    ],
  },
]
