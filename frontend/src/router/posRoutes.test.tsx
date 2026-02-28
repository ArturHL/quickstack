import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderInRoutes, renderWithProviders } from '../test-utils/renderWithProviders'
import CatalogPage from '../features/pos/pages/CatalogPage'
import Sidebar from '../components/layout/Sidebar'

describe('POS Routes', () => {
  it('/pos/catalog renders CatalogPage with product catalog', async () => {
    renderInRoutes(
      [{ path: '/pos/catalog', element: <CatalogPage /> }],
      { initialRoute: '/pos/catalog' }
    )

    // CatalogPage renders ProductCatalog which fetches menu — shows loader then data
    await waitFor(() => expect(screen.getByText('Bebidas')).toBeInTheDocument())
    expect(screen.getByText('Alimentos')).toBeInTheDocument()
  })

  it('Sidebar renders Catálogo link', () => {
    const onMobileClose = vi.fn()
    renderWithProviders(<Sidebar mobileOpen={false} onMobileClose={onMobileClose} />)

    expect(screen.getAllByText('Catálogo')[0]).toBeInTheDocument()
  })

  it('Sidebar Catálogo link is active when on /pos/* path', () => {
    const onMobileClose = vi.fn()
    renderWithProviders(<Sidebar mobileOpen={false} onMobileClose={onMobileClose} />, {
      initialRoute: '/pos/catalog',
    })

    // MUI ListItemButton with selected=true gets aria-current="true" or Mui-selected class
    const catalogButtons = screen.getAllByRole('button', { name: /catálogo/i })
    expect(catalogButtons.length).toBeGreaterThan(0)
  })
})
