import { describe, it, expect, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/renderWithProviders'
import OrderDetail from '../OrderDetail'
import { useAuthStore } from '../../../../stores/authStore'

function setUser(role: 'OWNER' | 'MANAGER' | 'CASHIER' | 'WAITER') {
    useAuthStore.setState({
        isAuthenticated: true,
        isLoading: false,
        accessToken: 'test-token',
        user: { id: 'user-1', email: 'test@test.com', fullName: 'Test User', role, tenantId: 'tenant-1' },
    })
}

beforeEach(() => {
    useAuthStore.setState({
        isAuthenticated: false,
        isLoading: false,
        accessToken: null,
        user: null,
    })
})

describe('OrderDetail', () => {
    it('renders order details', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-001/)).toBeInTheDocument()
        })
        expect(screen.getByText('Hamburguesa Clásica')).toBeInTheDocument()
    })

    it('shows order items with modifiers', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-ready" />)
        await waitFor(() => {
            expect(screen.getByText('Café Americano')).toBeInTheDocument()
        })
        expect(screen.getByText('Pan Dulce')).toBeInTheDocument()
        expect(screen.getByText('Leche Extra')).toBeInTheDocument()
    })

    it('shows cancel button for PENDING orders when user is MANAGER', async () => {
        setUser('MANAGER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        await waitFor(() => {
            expect(screen.getByLabelText('cancelar orden')).toBeInTheDocument()
        })
    })

    it('does not show cancel button for CASHIER on PENDING orders', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        await waitFor(() => {
            expect(screen.getByText(/ORD-20260228-001/)).toBeInTheDocument()
        })
        expect(screen.queryByLabelText('cancelar orden')).not.toBeInTheDocument()
    })

    it('shows mark ready button for IN_PROGRESS orders when MANAGER', async () => {
        setUser('MANAGER')
        renderWithProviders(<OrderDetail orderId="order-2" />)
        await waitFor(() => {
            expect(screen.getByLabelText('marcar como listo')).toBeInTheDocument()
        })
    })

    it('shows cobrar button for READY orders', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-ready" />)
        await waitFor(() => {
            expect(screen.getByLabelText('cobrar')).toBeInTheDocument()
        })
    })

    it('shows payments section for completed orders', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-completed" />)
        await waitFor(() => {
            expect(screen.getByText('Pagos')).toBeInTheDocument()
        })
        expect(screen.getByText('CASH')).toBeInTheDocument()
    })

    it('shows back button', async () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        await waitFor(() => {
            expect(screen.getByText('Volver a Pedidos')).toBeInTheDocument()
        })
    })

    it('shows loading state', () => {
        setUser('CASHIER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('handles cancel order action', async () => {
        const user = userEvent.setup()
        setUser('MANAGER')
        renderWithProviders(<OrderDetail orderId="order-1" />)
        await waitFor(() => {
            expect(screen.getByLabelText('cancelar orden')).toBeInTheDocument()
        })
        await user.click(screen.getByLabelText('cancelar orden'))
        // The mutation should have been called (no error thrown)
    })
})
