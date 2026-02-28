import { Box, Button, Typography } from '@mui/material'
import { CheckCircleOutline } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'

export default function OrderConfirmationPage() {
  const navigate = useNavigate()

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      p={6}
      gap={2}
    >
      <CheckCircleOutline sx={{ fontSize: 80, color: 'success.main' }} />
      <Typography variant="h5">Pedido Completado</Typography>
      <Typography variant="body1" color="text.secondary">
        El pago fue registrado exitosamente.
      </Typography>
      <Button
        variant="contained"
        size="large"
        onClick={() => navigate('/pos/new')}
      >
        Nueva Venta
      </Button>
    </Box>
  )
}
