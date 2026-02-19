import { render, type RenderOptions } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider } from '@mui/material/styles'
import theme from '../theme/theme'

const routerFutureFlags = {
  v7_startTransition: true,
  v7_relativeSplatPath: true,
} as const

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  })
}

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  initialRoute?: string
}

export function renderWithProviders(
  ui: React.ReactElement,
  { initialRoute = '/', ...options }: CustomRenderOptions = {}
) {
  const testQueryClient = createTestQueryClient()

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={[initialRoute]} future={routerFutureFlags}>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>
      </ThemeProvider>
    </MemoryRouter>
  )

  return render(ui, { wrapper: Wrapper, ...options })
}

interface RouteConfig {
  path: string
  element: React.ReactElement
}

export function renderInRoutes(
  routes: RouteConfig[],
  { initialRoute = '/' }: { initialRoute?: string } = {}
) {
  const testQueryClient = createTestQueryClient()

  return render(
    <MemoryRouter initialEntries={[initialRoute]} future={routerFutureFlags}>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={testQueryClient}>
          <Routes>
            {routes.map(({ path, element }) => (
              <Route key={path} path={path} element={element} />
            ))}
          </Routes>
        </QueryClientProvider>
      </ThemeProvider>
    </MemoryRouter>
  )
}
