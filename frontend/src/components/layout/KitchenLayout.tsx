import { Box, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

export default function KitchenLayout() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', bgcolor: '#111827', color: 'white' }}>
            {/* KDS Header - High Contrast */}
            <Box sx={{ p: 2, bgcolor: '#000000', borderBottom: '2px solid #374151', display: 'flex', justifyContent: 'space-between' }}>
                <Typography variant="h5" fontWeight={700} letterSpacing={1}>SISTEMA KDS</Typography>
                <Typography variant="h6" color="var(--color-success)">EN LINEA</Typography>
            </Box>

            {/* Main KDS Content Area - Kanban Board Style */}
            <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
                <Outlet />
            </Box>
        </Box>
    )
}
