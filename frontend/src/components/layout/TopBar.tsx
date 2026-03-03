import { AppBar, Toolbar, Typography, Button, Box, IconButton } from '@mui/material'
import { Menu as MenuIcon, Logout as LogoutIcon } from '@mui/icons-material'
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
        backgroundColor: 'primary.main',
      }}
    >
      <Toolbar>
        <IconButton
          color="inherit"
          aria-label="abrir menu"
          edge="start"
          onClick={onMenuClick}
          sx={{ mr: 2, display: { sm: 'none' } }}
        >
          <MenuIcon />
        </IconButton>

        <Typography
          variant="h6"
          component="div"
          sx={{ flexGrow: 0, mr: 2, whiteSpace: 'nowrap' }}
        >
          QuickStack POS
        </Typography>

        <BranchSelector />

        <Box sx={{ flexGrow: 1 }} />

        <Typography
          variant="body2"
          sx={{ mr: 1, display: { xs: 'none', sm: 'block' } }}
          noWrap
        >
          {displayName}
        </Typography>

        {/* Mobile: solo ícono */}
        <IconButton
          color="inherit"
          onClick={() => logout()}
          disabled={isPending}
          aria-label="cerrar sesión"
          sx={{ display: { xs: 'flex', sm: 'none' } }}
        >
          <LogoutIcon />
        </IconButton>

        {/* Desktop: botón con texto */}
        <Button
          color="inherit"
          onClick={() => logout()}
          disabled={isPending}
          startIcon={<LogoutIcon />}
          sx={{ textTransform: 'none', display: { xs: 'none', sm: 'flex' } }}
        >
          Cerrar sesión
        </Button>
      </Toolbar>
    </AppBar>
  )
}
