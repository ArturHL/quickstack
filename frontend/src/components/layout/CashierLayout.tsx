import { Box, Button, Divider, Typography } from '@mui/material'
import { LogoutOutlined as LogoutIcon } from '@mui/icons-material'
import { Outlet } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import { useLogout } from '../../hooks/useAuthQuery'

export default function CashierLayout() {
    const user = useAuthStore((s) => s.user)
    const { mutate: logout, isPending } = useLogout()
    const displayName = user?.fullName || user?.email || 'Usuario'

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', bgcolor: 'var(--surface-base)', overflow: 'hidden' }}>
            {/* Header bar */}
            <Box sx={{
                px: 2,
                py: 1,
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                borderBottom: 1,
                borderColor: 'divider',
                bgcolor: 'var(--surface-card)',
                flexShrink: 0,
            }}>
                <Typography variant="subtitle2" fontWeight={700}>
                    QuickStack POS
                </Typography>
                <Divider orientation="vertical" flexItem sx={{ my: 0.5 }} />
                <Typography variant="caption" color="text.secondary" noWrap>
                    {displayName}
                </Typography>
                <Box sx={{ flexGrow: 1 }} />
                <Button
                    variant="text"
                    size="small"
                    onClick={() => logout()}
                    disabled={isPending}
                    startIcon={<LogoutIcon fontSize="small" />}
                    aria-label="cerrar sesión"
                    sx={{ color: 'text.secondary', fontSize: '0.8125rem' }}
                >
                    Salir
                </Button>
            </Box>

            {/* CashierPos manages its own internal layout */}
            <Box sx={{ flexGrow: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
                <Outlet />
            </Box>
        </Box>
    )
}
