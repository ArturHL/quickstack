import { setupServer } from 'msw/node'
import { authHandlers } from './handlers/authHandlers'
import { menuHandlers } from './handlers/menuHandlers'
import { tableHandlers } from './handlers/tableHandlers'
import { customerHandlers } from './handlers/customerHandlers'

export const server = setupServer(...authHandlers, ...menuHandlers, ...tableHandlers, ...customerHandlers)
