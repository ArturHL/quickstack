import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Divider,
  Typography,
} from '@mui/material'
import {
  Home as HomeIcon,
  ShoppingCart as ShoppingCartIcon,
  Receipt as ReceiptIcon,
  Inventory as InventoryIcon,
  Assessment as AssessmentIcon,
  People as PeopleIcon,
  Store as StoreIcon,
} from '@mui/icons-material'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import type { AuthUser } from '../../types/auth'

type UserRole = AuthUser['role']

const ROLE_RANK: Record<UserRole, number> = {
  WAITER: 0,
  CASHIER: 1,
  MANAGER: 2,
  OWNER: 3,
}

function hasMinRole(userRole: UserRole | undefined, minRole: UserRole): boolean {
  if (!userRole) return false
  return (ROLE_RANK[userRole] ?? -1) >= ROLE_RANK[minRole]
}

const DRAWER_WIDTH = 240

interface SidebarProps {
  mobileOpen: boolean
  onMobileClose: () => void
}

export default function Sidebar({ mobileOpen, onMobileClose }: SidebarProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthStore((s) => s.user)

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
          <ListItemButton
            selected={location.pathname.startsWith('/orders')}
            onClick={() => handleNavigate('/orders')}
          >
            <ListItemIcon>
              <ReceiptIcon />
            </ListItemIcon>
            <ListItemText primary="Pedidos" />
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

      {/* Admin section — CASHIER+ */}
      {hasMinRole(user?.role, 'CASHIER') && (
        <>
          <Divider />
          <Typography variant="caption" color="text.secondary" sx={{ px: 2, pt: 1, display: 'block' }}>
            Administración
          </Typography>
          <List>
            {hasMinRole(user?.role, 'MANAGER') && (
              <ListItem disablePadding>
                <ListItemButton
                  selected={location.pathname.startsWith('/admin/products')}
                  onClick={() => handleNavigate('/admin/products')}
                >
                  <ListItemIcon>
                    <InventoryIcon />
                  </ListItemIcon>
                  <ListItemText primary="Productos" />
                </ListItemButton>
              </ListItem>
            )}

            {hasMinRole(user?.role, 'OWNER') && (
              <ListItem disablePadding>
                <ListItemButton
                  selected={location.pathname.startsWith('/admin/branches')}
                  onClick={() => handleNavigate('/admin/branches')}
                >
                  <ListItemIcon>
                    <StoreIcon />
                  </ListItemIcon>
                  <ListItemText primary="Sucursales" />
                </ListItemButton>
              </ListItem>
            )}

            <ListItem disablePadding>
              <ListItemButton
                selected={location.pathname.startsWith('/admin/customers')}
                onClick={() => handleNavigate('/admin/customers')}
              >
                <ListItemIcon>
                  <PeopleIcon />
                </ListItemIcon>
                <ListItemText primary="Clientes" />
              </ListItemButton>
            </ListItem>
          </List>
        </>
      )}
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
