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

        <Typography variant="h6" component="div" sx={{ flexGrow: 0, mr: 3 }}>
          QuickStack POS
        </Typography>

        <BranchSelector />

        <Box sx={{ flexGrow: 1 }} />

        <Typography variant="body1" sx={{ mr: 2 }}>
          {displayName}
        </Typography>

        <Button
          color="inherit"
          onClick={() => logout()}
          disabled={isPending}
          startIcon={<LogoutIcon />}
          sx={{ textTransform: 'none' }}
        >
          Cerrar sesión
        </Button>
      </Toolbar>
    </AppBar>
  )
}
