import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement } from 'react'
import { describe, it, expect } from 'vitest'
import { useMenuQuery } from '../useMenuQuery'
import { server } from '../../../../mocks/server'
import { mockMenuResponse, menuErrorHandlers } from '../../../../mocks/handlers/menuHandlers'

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })
}

function createWrapper(queryClient: QueryClient) {
  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children)
}

describe('useMenuQuery', () => {
  it('returns isLoading=true initially', () => {
    const queryClient = createTestQueryClient()
    const { result } = renderHook(() => useMenuQuery(), {
      wrapper: createWrapper(queryClient),
    })

    expect(result.current.isLoading).toBe(true)
  })

  it('returns data on success', async () => {
    const queryClient = createTestQueryClient()
    const { result } = renderHook(() => useMenuQuery(), {
      wrapper: createWrapper(queryClient),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual(mockMenuResponse)
  })

  it('returns isError=true on failure', async () => {
    server.use(menuErrorHandlers.serverError())
    const queryClient = createTestQueryClient()
    const { result } = renderHook(() => useMenuQuery(), {
      wrapper: createWrapper(queryClient),
    })

    await waitFor(() => expect(result.current.isError).toBe(true), { timeout: 5000 })
  })

  it('serves data from cache without fetching again', async () => {
    const queryClient = createTestQueryClient()
    queryClient.setQueryData(['menu'], mockMenuResponse)

    const { result } = renderHook(() => useMenuQuery(), {
      wrapper: createWrapper(queryClient),
    })

    // Data should be immediately available from cache — no loading state
    expect(result.current.isLoading).toBe(false)
    expect(result.current.data).toEqual(mockMenuResponse)
  })
})
