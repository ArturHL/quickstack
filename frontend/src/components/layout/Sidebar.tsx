import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Divider,
} from '@mui/material'
import {
  Home as HomeIcon,
  ShoppingCart as ShoppingCartIcon,
  Receipt as ReceiptIcon,
  Inventory as InventoryIcon,
  Assessment as AssessmentIcon,
} from '@mui/icons-material'
import { useNavigate, useLocation } from 'react-router-dom'

const DRAWER_WIDTH = 240

interface SidebarProps {
  mobileOpen: boolean
  onMobileClose: () => void
}

export default function Sidebar({ mobileOpen, onMobileClose }: SidebarProps) {
  const navigate = useNavigate()
  const location = useLocation()

  const handleNavigate = (path: string) => {
    navigate(path)
    onMobileClose()
  }

  const drawerContent = (
    <>
      <Toolbar />
      <Divider />
      <List>
        <ListItem disablePadding>
          <ListItemButton
            selected={location.pathname === '/dashboard'}
            onClick={() => handleNavigate('/dashboard')}
          >
            <ListItemIcon>
              <HomeIcon />
            </ListItemIcon>
            <ListItemText primary="Dashboard" />
          </ListItemButton>
        </ListItem>

        <ListItem disablePadding>
          <ListItemButton
            selected={location.pathname.startsWith('/pos')}
            onClick={() => handleNavigate('/pos/catalog')}
          >
            <ListItemIcon>
              <ShoppingCartIcon />
            </ListItemIcon>
            <ListItemText primary="Catálogo" />
          </ListItemButton>
        </ListItem>

        <ListItem disablePadding>
          <ListItemButton disabled>
            <ListItemIcon>
              <ReceiptIcon />
            </ListItemIcon>
            <ListItemText primary="Pedidos" secondary="Próximamente" />
          </ListItemButton>
        </ListItem>

        <ListItem disablePadding>
          <ListItemButton disabled>
            <ListItemIcon>
              <InventoryIcon />
            </ListItemIcon>
            <ListItemText primary="Inventario" secondary="Próximamente" />
          </ListItemButton>
        </ListItem>

        <ListItem disablePadding>
          <ListItemButton disabled>
            <ListItemIcon>
              <AssessmentIcon />
            </ListItemIcon>
            <ListItemText primary="Reportes" secondary="Próximamente" />
          </ListItemButton>
        </ListItem>
      </List>
    </>
  )

  return (
    <>
      {/* Mobile drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{
          keepMounted: true, // Better open performance on mobile
        }}
        sx={{
          display: { xs: 'block', sm: 'none' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: DRAWER_WIDTH,
          },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* Desktop drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', sm: 'block' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: DRAWER_WIDTH,
          },
        }}
        open
      >
        {drawerContent}
      </Drawer>
    </>
  )
}
