import { Box, Typography, Card, CardContent, Grid, Skeleton } from '@mui/material'
import {
  ShoppingCartOutlined,
  ReceiptLongOutlined,
  BarChartOutlined,
  ArrowForwardOutlined,
} from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import { useBranchStore } from '../features/pos/stores/branchStore'
import { useDailySummaryQuery } from '../features/reports/hooks/useDailySummaryQuery'

function todayISO(): string {
  return new Date().toISOString().split('T')[0]
}

function formatDate(): string {
  return new Date().toLocaleDateString('es-MX', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

interface StatCardProps {
  label: string
  value: string
  loading?: boolean
}

function StatCard({ label, value, loading }: StatCardProps) {
  return (
    <Card className="comanda-edge">
      <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
        <Typography
          sx={{
            display: 'block',
            mb: 1,
            fontSize: '0.6875rem',
            fontWeight: 600,
            letterSpacing: '0.07em',
            textTransform: 'uppercase',
            color: 'text.secondary',
          }}
        >
          {label}
        </Typography>
        {loading ? (
          <Skeleton variant="text" width={90} height={40} sx={{ transform: 'none' }} />
        ) : (
          <Typography
            className="tabular-nums"
            sx={{
              fontSize: '1.75rem',
              fontWeight: 700,
              letterSpacing: '-0.03em',
              lineHeight: 1,
              color: 'text.primary',
            }}
          >
            {value}
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}

interface QuickActionProps {
  icon: React.ReactNode
  label: string
  description: string
  onClick: () => void
}

function QuickAction({ icon, label, description, onClick }: QuickActionProps) {
  return (
    <Card
      onClick={onClick}
      sx={{
        cursor: 'pointer',
        transition: 'border-color 0.15s ease, background-color 0.15s ease',
        '&:hover': {
          borderColor: 'rgba(0, 0, 0, 0.18)',
          backgroundColor: '#FAFAF9',
        },
      }}
    >
      <CardContent
        sx={{
          p: 2.25,
          '&:last-child': { pb: 2.25 },
          display: 'flex',
          alignItems: 'center',
          gap: 1.75,
        }}
      >
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: '8px',
            border: '1px solid rgba(0,0,0,0.08)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'text.secondary',
            flexShrink: 0,
          }}
        >
          {icon}
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600} color="text.primary">
            {label}
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            {description}
          </Typography>
        </Box>
        <ArrowForwardOutlined sx={{ fontSize: 15, color: 'text.disabled', flexShrink: 0 }} />
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const activeBranchId = useBranchStore((s) => s.activeBranchId)
  const { data, isLoading } = useDailySummaryQuery(activeBranchId, todayISO())

  const displayName = user?.fullName?.split(' ')[0] || user?.email || 'Usuario'

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography
          sx={{
            fontSize: '1.375rem',
            fontWeight: 600,
            letterSpacing: '-0.02em',
            color: 'text.primary',
            mb: 0.375,
          }}
        >
          Hola, {displayName}
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ textTransform: 'capitalize' }}
        >
          {formatDate()}
        </Typography>
      </Box>

      {/* Stats */}
      <Box sx={{ mb: 4 }}>
        <Typography
          sx={{
            display: 'block',
            mb: 1.5,
            fontSize: '0.6875rem',
            fontWeight: 600,
            letterSpacing: '0.07em',
            textTransform: 'uppercase',
            color: 'text.disabled',
          }}
        >
          Resumen de hoy
        </Typography>

        {!activeBranchId ? (
          <Card>
            <CardContent sx={{ p: 2.25, '&:last-child': { pb: 2.25 } }}>
              <Typography variant="body2" color="text.secondary">
                Selecciona una sucursal para ver el resumen del día.
              </Typography>
            </CardContent>
          </Card>
        ) : (
          <Grid container spacing={2}>
            <Grid item xs={12} sm={4}>
              <StatCard
                label="Total ventas"
                value={data ? `$${data.totalSales.toFixed(2)}` : '$0.00'}
                loading={isLoading}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <StatCard
                label="Pedidos"
                value={data ? String(data.totalOrders) : '0'}
                loading={isLoading}
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <StatCard
                label="Ticket promedio"
                value={data ? `$${data.averageTicket.toFixed(2)}` : '$0.00'}
                loading={isLoading}
              />
            </Grid>
          </Grid>
        )}
      </Box>

      {/* Quick actions */}
      <Box>
        <Typography
          sx={{
            display: 'block',
            mb: 1.5,
            fontSize: '0.6875rem',
            fontWeight: 600,
            letterSpacing: '0.07em',
            textTransform: 'uppercase',
            color: 'text.disabled',
          }}
        >
          Accesos rápidos
        </Typography>
        <Grid container spacing={2}>
          <Grid item xs={12} sm={6} md={4}>
            <QuickAction
              icon={<ShoppingCartOutlined sx={{ fontSize: 18 }} />}
              label="Nueva orden"
              description="Iniciar venta desde el catálogo"
              onClick={() => navigate('/pos/catalog')}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <QuickAction
              icon={<ReceiptLongOutlined sx={{ fontSize: 18 }} />}
              label="Pedidos del día"
              description="Ver y gestionar pedidos activos"
              onClick={() => navigate('/orders')}
            />
          </Grid>
          <Grid item xs={12} sm={6} md={4}>
            <QuickAction
              icon={<BarChartOutlined sx={{ fontSize: 18 }} />}
              label="Reporte diario"
              description="Ventas, productos y métricas"
              onClick={() => navigate('/admin/reports')}
            />
          </Grid>
        </Grid>
      </Box>
    </Box>
  )
}
