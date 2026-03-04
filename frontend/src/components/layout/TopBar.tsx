import { AppBar, Toolbar, Box, IconButton, Button, Typography, Divider } from '@mui/material'
import { MenuOutlined as MenuIcon, LogoutOutlined as LogoutIcon } from '@mui/icons-material'
import { useAuthStore } from '../../stores/authStore'
import { useLogout } from '../../hooks/useAuthQuery'
import BranchSelector from '../../features/branches/components/BranchSelector'

interface TopBarProps {
  onMenuClick: () => void
}

export default function TopBar({ onMenuClick }: TopBarProps) {
  const user = useAuthStore((s) => s.user)
  const { mutate: logout, isPending } = useLogout()

  const displayName = user?.fullName || user?.email || 'Usuario'

  return (
    <AppBar
      position="fixed"
      sx={{
        zIndex: (theme) => theme.zIndex.drawer + 1,
      }}
    >
      <Toolbar sx={{ gap: 1.5, minHeight: '52px !important', px: { xs: 1.5, sm: 2.5 } }}>
        <IconButton
          aria-label="abrir menu"
          edge="start"
          onClick={onMenuClick}
          size="small"
          sx={{ display: { sm: 'none' }, color: 'text.secondary' }}
        >
          <MenuIcon fontSize="small" />
        </IconButton>

        <BranchSelector />

        <Box sx={{ flexGrow: 1 }} />

        <Divider orientation="vertical" flexItem sx={{ my: 1.25, borderColor: 'rgba(0,0,0,0.08)' }} />

        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: { xs: 'none', sm: 'block' }, fontWeight: 500 }}
          noWrap
        >
          {displayName}
        </Typography>

        <IconButton
          onClick={() => logout()}
          disabled={isPending}
          aria-label="cerrar sesión"
          size="small"
          sx={{ color: 'text.disabled', display: { xs: 'flex', sm: 'none' } }}
        >
          <LogoutIcon fontSize="small" />
        </IconButton>

        <Button
          variant="text"
          onClick={() => logout()}
          disabled={isPending}
          startIcon={<LogoutIcon sx={{ fontSize: '15px !important' }} />}
          sx={{
            display: { xs: 'none', sm: 'flex' },
            color: 'text.disabled',
            fontSize: '0.8125rem',
            fontWeight: 400,
            px: 1,
            py: 0.5,
            minWidth: 0,
            '&:hover': { color: 'text.secondary', bgcolor: 'rgba(0,0,0,0.04)' },
          }}
        >
          Salir
        </Button>
      </Toolbar>
    </AppBar>
  )
}
