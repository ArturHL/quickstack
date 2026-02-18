import { QueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      onError: (error) => {
        const axiosErr = error as AxiosError
        const status = axiosErr.response?.status

        if (!status || status >= 500 || axiosErr.code === 'ERR_NETWORK') {
          const event = new CustomEvent('global-query-error', {
            detail: { error },
          })
          window.dispatchEvent(event)
        }
      },
    },
  },
})
