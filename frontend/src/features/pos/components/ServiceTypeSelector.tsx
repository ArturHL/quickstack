import { useNavigate } from 'react-router-dom'
import { Box, Card, CardActionArea, CardContent, Grid, Typography } from '@mui/material'
import { DeliveryDining, Restaurant, Store, TakeoutDining } from '@mui/icons-material'
import { useCartStore } from '../stores/cartStore'
import type { ServiceType } from '../types/Cart'

const SERVICE_TYPES: { type: ServiceType; label: string; icon: React.ReactNode }[] = [
  { type: 'DINE_IN', label: 'Mesa', icon: <Restaurant sx={{ fontSize: 48 }} /> },
  { type: 'COUNTER', label: 'Mostrador', icon: <Store sx={{ fontSize: 48 }} /> },
  { type: 'DELIVERY', label: 'Delivery', icon: <DeliveryDining sx={{ fontSize: 48 }} /> },
  { type: 'TAKEOUT', label: 'Para llevar', icon: <TakeoutDining sx={{ fontSize: 48 }} /> },
]

export default function ServiceTypeSelector() {
  const navigate = useNavigate()
  const setServiceDetails = useCartStore((s) => s.setServiceDetails)

  const handleSelect = (type: ServiceType) => {
    setServiceDetails(type)
    if (type === 'DINE_IN') {
      navigate('/pos/new/table')
    } else if (type === 'DELIVERY' || type === 'TAKEOUT') {
      navigate('/pos/new/customer')
    } else {
      navigate('/pos/catalog')
    }
  }

  return (
    <Box p={2}>
      <Typography variant="h5" gutterBottom>
        Tipo de Servicio
      </Typography>
      <Grid container spacing={3}>
        {SERVICE_TYPES.map(({ type, label, icon }) => (
          <Grid item xs={12} sm={6} md={3} key={type}>
            <Card>
              <CardActionArea
                onClick={() => handleSelect(type)}
                aria-label={label}
                sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: 3 }}
              >
                <CardContent
                  sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}
                >
                  {icon}
                  <Typography variant="h6">{label}</Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
