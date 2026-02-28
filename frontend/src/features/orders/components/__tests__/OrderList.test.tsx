import { describe, it, expect } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import OrderList from '../OrderList'

describe('OrderList', () => {
    it('renders the title', async () => {
        renderWithProviders(<OrderList />)
        expect(screen.getByText('Pedidos del Día')).toBeInTheDocument()
    })

    it('renders orders from the API', async () => {
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-001/)).toBeInTheDocument()
        })
        expect(screen.getByText(/ORD-20260228-002/)).toBeInTheDocument()
        expect(screen.getByText(/ORD-20260228-003/)).toBeInTheDocument()
    })

    it('shows status chips for each order', async () => {
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getByText('Pendiente')).toBeInTheDocument()
        })
        expect(screen.getByText('En Progreso')).toBeInTheDocument()
        expect(screen.getByText('Listo')).toBeInTheDocument()
    })

    it('shows service type chips', async () => {
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getAllByText('Mostrador').length).toBeGreaterThan(0)
        })
        expect(screen.getByText('En Mesa')).toBeInTheDocument()
    })

    it('shows order totals', async () => {
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getByText('Pendiente')).toBeInTheDocument()
        })
        // Totals are rendered as "$X.XX" inside Typography elements
        const allText = document.body.textContent ?? ''
        expect(allText).toContain('116.00')
        expect(allText).toContain('232.00')
    })

    it('filters by status when a status is selected', async () => {
        const user = userEvent.setup()
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-001/)).toBeInTheDocument()
        })

        // Open MUI Select dropdown
        const selectButton = screen.getByLabelText('filtro estado')
        await user.click(selectButton)

        // Click "Listo" option in the dropdown
        const readyOption = await screen.findByRole('option', { name: 'Listo' })
        await user.click(readyOption)

        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-003/)).toBeInTheDocument()
        })
    })

    it('shows date filter input', async () => {
        renderWithProviders(<OrderList />)
        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-001/)).toBeInTheDocument()
        })
        const dateInput = screen.getByLabelText('fecha')
        expect(dateInput).toBeInTheDocument()
    })

    it('shows loading state initially', () => {
        renderWithProviders(<OrderList />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })
})
