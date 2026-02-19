import { Box, Typography, Card, CardContent, Grid } from '@mui/material'
import { CheckCircle as CheckCircleIcon } from '@mui/icons-material'
import { useAuthStore } from '../stores/authStore'

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)

  const displayName = user?.fullName || user?.email || 'Usuario'

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom>
        Dashboard
      </Typography>

      <Typography variant="h6" color="text.secondary" gutterBottom>
        Bienvenido, {displayName}
      </Typography>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={6} lg={4}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <CheckCircleIcon color="success" sx={{ mr: 1, fontSize: 40 }} />
                <Typography variant="h5" component="div">
                  Sistema Activo
                </Typography>
              </Box>
              <Typography variant="body1" color="text.secondary">
                POS funcionando
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Todas las funcionalidades operativas
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" component="div" gutterBottom>
                QuickStack POS
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Sistema de punto de venta para restaurantes
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ mt: 2, display: 'block' }}>
                Phase 0.4 — Frontend Base + Integración Auth
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" component="div" gutterBottom>
                Próximas Funcionalidades
              </Typography>
              <Typography variant="body2" color="text.secondary" component="div">
                <ul style={{ paddingLeft: '1.5rem', marginTop: '0.5rem' }}>
                  <li>Catálogo de productos (Phase 1)</li>
                  <li>Gestión de pedidos (Phase 1)</li>
                  <li>Inventario automático (Phase 2)</li>
                  <li>Reportes de ventas (Phase 4)</li>
                </ul>
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}
