import CatalogPage from '../features/pos/pages/CatalogPage'
import NewOrderPage from '../features/pos/pages/NewOrderPage'
import TableSelectionPage from '../features/pos/pages/TableSelectionPage'
import CustomerSelectionPage from '../features/pos/pages/CustomerSelectionPage'
import CartPage from '../features/pos/pages/CartPage'
import PaymentPage from '../features/pos/pages/PaymentPage'
import OrderConfirmationPage from '../features/pos/pages/OrderConfirmationPage'

export const posRoutes = [
  { path: 'catalog', element: <CatalogPage /> },
  { path: 'new', element: <NewOrderPage /> },
  { path: 'new/table', element: <TableSelectionPage /> },
  { path: 'new/customer', element: <CustomerSelectionPage /> },
  { path: 'cart', element: <CartPage /> },
  { path: 'payment', element: <PaymentPage /> },
  { path: 'confirmation', element: <OrderConfirmationPage /> },
]
