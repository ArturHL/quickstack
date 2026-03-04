import { Box, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

export default function WaiterLayout() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', bgcolor: 'var(--surface-base)' }}>
            {/* Top App Bar for Waiter */}
            <Box sx={{ p: 2, bgcolor: 'var(--surface-card)', borderBottom: '1px solid var(--border-subtle)' }}>
                <Typography variant="h6" fontWeight={700}>Piso & Mesas</Typography>
            </Box>

            {/* Main Content Area */}
            <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
                <Outlet />
            </Box>

            {/* Bottom Navigation Navbar - Mobile First */}
            <Box sx={{
                display: 'flex',
                justifyContent: 'space-around',
                p: 2,
                bgcolor: 'var(--surface-card)',
                borderTop: '1px solid var(--border-subtle)',
                pb: 'env(safe-area-inset-bottom, 16px)'
            }}>
                <Typography variant="body2" color="text.secondary">Mesas</Typography>
                <Typography variant="body2" color="text.secondary">Menú</Typography>
                <Typography variant="body2" color="text.secondary">Órdenes</Typography>
            </Box>
        </Box>
    )
}
