import { setupServer } from 'msw/node'
import { authHandlers } from './handlers/authHandlers'
import { menuHandlers } from './handlers/menuHandlers'

export const server = setupServer(...authHandlers, ...menuHandlers)
