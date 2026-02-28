import { setupServer } from 'msw/node'
import { authHandlers } from './handlers/authHandlers'
import { menuHandlers } from './handlers/menuHandlers'
import { tableHandlers } from './handlers/tableHandlers'
import { customerHandlers } from './handlers/customerHandlers'
import { orderHandlers } from './handlers/orderHandlers'
import { productHandlers } from './handlers/productHandlers'
import { branchHandlers } from './handlers/branchHandlers'

export const server = setupServer(
  ...authHandlers,
  ...menuHandlers,
  ...tableHandlers,
  ...customerHandlers,
  ...orderHandlers,
  ...productHandlers,
  ...branchHandlers,
)
