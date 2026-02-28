import CatalogPage from '../features/pos/pages/CatalogPage'
import NewOrderPage from '../features/pos/pages/NewOrderPage'
import TableSelectionPage from '../features/pos/pages/TableSelectionPage'
import CustomerSelectionPage from '../features/pos/pages/CustomerSelectionPage'
import CartPage from '../features/pos/pages/CartPage'

export const posRoutes = [
  { path: '/pos/catalog', element: <CatalogPage /> },
  { path: '/pos/new', element: <NewOrderPage /> },
  { path: '/pos/new/table', element: <TableSelectionPage /> },
  { path: '/pos/new/customer', element: <CustomerSelectionPage /> },
  { path: '/pos/cart', element: <CartPage /> },
]
