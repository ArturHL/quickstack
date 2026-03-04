import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
} from '@mui/material'
import {
  HomeOutlined as HomeIcon,
  ShoppingCartOutlined as ShoppingCartIcon,
  ReceiptLongOutlined as ReceiptIcon,
  InventoryOutlined as InventoryIcon,
  BarChartOutlined as AssessmentIcon,
  PeopleOutlined as PeopleIcon,
  StoreOutlined as StoreIcon,
  CategoryOutlined as CategoryIcon,
  LocalOfferOutlined as LocalOfferIcon,
} from '@mui/icons-material'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../../stores/authStore'
import type { AuthUser } from '../../types/auth'

type UserRole = AuthUser['role']

const ROLE_RANK: Record<UserRole, number> = {
  WAITER: 0,
  KITCHEN: 1,
  CASHIER: 1,
  MANAGER: 2,
  OWNER: 3,
}

function hasMinRole(userRole: UserRole | undefined, minRole: UserRole): boolean {
  if (!userRole) return false
  return (ROLE_RANK[userRole] ?? -1) >= ROLE_RANK[minRole]
}

const DRAWER_WIDTH = 220

const S = {
  bg: '#18181B',
  textMuted: 'rgba(255, 255, 255, 0.55)',
  textActive: '#18181B',
  activeBg: '#FFFFFF',
  sectionLabel: 'rgba(255, 255, 255, 0.30)',
  hoverBg: 'rgba(255, 255, 255, 0.07)',
}

interface SidebarProps {
  mobileOpen: boolean
  onMobileClose: () => void
}

interface NavItemProps {
  icon: React.ReactNode
  label: string
  path: string
  active: boolean
  disabled?: boolean
  secondary?: string
  onClick: (path: string) => void
}

function NavItem({ icon, label, path, active, disabled, secondary, onClick }: NavItemProps) {
  return (
    <ListItem disablePadding sx={{ px: 1.5, mb: 0.25 }}>
      <ListItemButton
        selected={active}
        disabled={disabled}
        onClick={() => onClick(path)}
        sx={{
          borderRadius: '6px',
          py: 0.75,
          px: 1.25,
          gap: 1.25,
          color: active ? S.textActive : S.textMuted,
          backgroundColor: active ? `${S.activeBg} !important` : 'transparent',
          '&:hover': {
            backgroundColor: active ? S.activeBg : S.hoverBg,
          },
          '&.Mui-disabled': {
            opacity: 0.45,
            color: S.textMuted,
          },
          transition: 'background-color 0.13s ease, color 0.13s ease',
          minHeight: 0,
        }}
      >
        <ListItemIcon
          sx={{ minWidth: 0, color: 'inherit', '& svg': { fontSize: 17 } }}
        >
          {icon}
        </ListItemIcon>
        <ListItemText
          primary={label}
          secondary={secondary}
          primaryTypographyProps={{
            fontSize: '0.8125rem',
            fontWeight: active ? 600 : 400,
            lineHeight: '1.25rem',
          }}
          secondaryTypographyProps={{
            fontSize: '0.6875rem',
            color: S.sectionLabel,
            lineHeight: '1rem',
          }}
          sx={{ my: 0 }}
        />
      </ListItemButton>
    </ListItem>
  )
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <Typography
      sx={{
        px: 2.75,
        pt: 2,
        pb: 0.5,
        display: 'block',
        color: S.sectionLabel,
        fontSize: '0.625rem',
        fontWeight: 600,
        letterSpacing: '0.09em',
        textTransform: 'uppercase',
        fontFamily: '"Inter", sans-serif',
      }}
    >
      {children}
    </Typography>
  )
}

const drawerSx = {
  '& .MuiDrawer-paper': {
    boxSizing: 'border-box',
    width: DRAWER_WIDTH,
    backgroundColor: S.bg,
    border: 'none',
    borderRight: '1px solid rgba(255, 255, 255, 0.05)',
  },
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
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', bgcolor: S.bg }}>
      {/* Brand */}
      <Box sx={{ px: 2.75, pt: 2.5, pb: 1 }}>
        <Typography
          sx={{
            color: '#FFFFFF',
            fontWeight: 700,
            fontSize: '0.9375rem',
            letterSpacing: '-0.01em',
            lineHeight: 1.2,
            fontFamily: '"Inter", sans-serif',
          }}
        >
          QuickStack
        </Typography>
        <Typography
          sx={{
            color: S.sectionLabel,
            fontSize: '0.6875rem',
            mt: 0.25,
            fontFamily: '"Inter", sans-serif',
          }}
        >
          Punto de Venta
        </Typography>
      </Box>

      <SectionLabel>Operaciones</SectionLabel>
      <List disablePadding>
        <NavItem
          icon={<HomeIcon />}
          label="Dashboard"
          path="/dashboard"
          active={location.pathname === '/dashboard'}
          onClick={handleNavigate}
        />
        <NavItem
          icon={<ShoppingCartIcon />}
          label="Catálogo"
          path="/pos/catalog"
          active={location.pathname.startsWith('/pos')}
          onClick={handleNavigate}
        />
        <NavItem
          icon={<ReceiptIcon />}
          label="Pedidos"
          path="/orders"
          active={location.pathname.startsWith('/orders')}
          onClick={handleNavigate}
        />
        <NavItem
          icon={<InventoryIcon />}
          label="Inventario"
          path="/inventory"
          active={false}
          disabled
          secondary="Próximamente"
          onClick={handleNavigate}
        />
        {hasMinRole(user?.role, 'MANAGER') && (
          <NavItem
            icon={<AssessmentIcon />}
            label="Reportes"
            path="/admin/reports"
            active={location.pathname.startsWith('/admin/reports')}
            onClick={handleNavigate}
          />
        )}
      </List>

      {hasMinRole(user?.role, 'CASHIER') && (
        <>
          <SectionLabel>Administración</SectionLabel>
          <List disablePadding>
            {hasMinRole(user?.role, 'MANAGER') && (
              <NavItem
                icon={<CategoryIcon />}
                label="Categorías"
                path="/admin/categories"
                active={location.pathname.startsWith('/admin/categories')}
                onClick={handleNavigate}
              />
            )}
            {hasMinRole(user?.role, 'MANAGER') && (
              <NavItem
                icon={<InventoryIcon />}
                label="Productos"
                path="/admin/products"
                active={location.pathname.startsWith('/admin/products')}
                onClick={handleNavigate}
              />
            )}
            {hasMinRole(user?.role, 'MANAGER') && (
              <NavItem
                icon={<LocalOfferIcon />}
                label="Combos"
                path="/admin/combos"
                active={location.pathname.startsWith('/admin/combos')}
                onClick={handleNavigate}
              />
            )}
            {hasMinRole(user?.role, 'OWNER') && (
              <NavItem
                icon={<StoreIcon />}
                label="Sucursales"
                path="/admin/branches"
                active={location.pathname.startsWith('/admin/branches')}
                onClick={handleNavigate}
              />
            )}
            <NavItem
              icon={<PeopleIcon />}
              label="Clientes"
              path="/admin/customers"
              active={location.pathname.startsWith('/admin/customers')}
              onClick={handleNavigate}
            />
          </List>
        </>
      )}

      <Box sx={{ flexGrow: 1 }} />
    </Box>
  )

  return (
    <>
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onMobileClose}
        ModalProps={{ keepMounted: true }}
        sx={{ display: { xs: 'block', sm: 'none' }, ...drawerSx }}
      >
        {drawerContent}
      </Drawer>

      <Drawer
        variant="permanent"
        sx={{ display: { xs: 'none', sm: 'block' }, ...drawerSx }}
        open
      >
        {drawerContent}
      </Drawer>
    </>
  )
}
