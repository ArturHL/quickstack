import { Box, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

export default function CashierLayout() {
    return (
        <Box sx={{ display: 'flex', height: '100vh', bgcolor: 'var(--surface-base)' }}>
            {/* Persistent Side Navigation for POS */}
            <Box sx={{
                width: 80,
                bgcolor: 'var(--surface-sidebar)',
                color: 'white',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                py: 3
            }}>
                <Typography variant="caption" sx={{ opacity: 0.5, mb: 4 }}>POS</Typography>
                {/* Nav Items will go here */}
                <Typography variant="caption" sx={{ mt: 'auto', opacity: 0.5 }}>Salir</Typography>
            </Box>

            {/* Main POS Content Area */}
            <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
                <Box sx={{ p: 2, borderBottom: '1px solid var(--border-subtle)', bgcolor: 'var(--surface-card)' }}>
                    <Typography variant="h6" fontWeight={700}>Terminal Principal</Typography>
                </Box>
                <Box sx={{ p: 2 }}>
                    <Outlet />
                </Box>
            </Box>
        </Box>
    )
}
