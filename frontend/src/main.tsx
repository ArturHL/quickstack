import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ThemeProvider } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import theme from './theme/theme'
import { queryClient } from './lib/queryClient'
import AppRouter from './router/AppRouter'
import { router } from './router/router'
import { registerNavigate } from './utils/imperativeNavigate'
import ErrorBoundary from './components/common/ErrorBoundary'
import GlobalErrorSnackbar from './components/common/GlobalErrorSnackbar'

registerNavigate((path) => {
  router.navigate(path)
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <QueryClientProvider client={queryClient}>
          <AppRouter />
          <GlobalErrorSnackbar />
          {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
        </QueryClientProvider>
      </ThemeProvider>
    </ErrorBoundary>
  </StrictMode>
)
