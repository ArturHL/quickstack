import { setupServer } from 'msw/node'
import { authHandlers } from './handlers/authHandlers'

export const server = setupServer(...authHandlers)
