import { Box, Card, CardContent, CircularProgress, Divider, Typography, Alert } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { orderApi } from '../api/orderApi'
import { usePosStore } from '../stores/posStore'
import { useCartStore } from '../stores/cartStore'
import { useRegisterPaymentMutation } from '../hooks/useRegisterPaymentMutation'
import PaymentForm from '../components/PaymentForm'

export default function PaymentPage() {
  const navigate = useNavigate()
  const currentOrderId = usePosStore((s) => s.currentOrderId)
  const clearCurrentOrder = usePosStore((s) => s.clearCurrentOrder)
  const clearCart = useCartStore((s) => s.clearCart)

  const { data: order, isLoading, isError } = useQuery({
    queryKey: ['order', currentOrderId],
    queryFn: () => orderApi.getOrder(currentOrderId!),
    enabled: !!currentOrderId,
  })

  const payment = useRegisterPaymentMutation()

  const handlePayment = async (amount: number) => {
    if (!order) return

    await payment.mutateAsync({
      orderId: order.id,
      paymentMethod: 'CASH',
      amount,
    })

    clearCart()
    clearCurrentOrder()
    navigate('/pos/confirmation')
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={6}>
        <CircularProgress />
      </Box>
    )
  }

  if (isError || !order) {
    return (
      <Box p={3}>
        <Alert severity="error">No se pudo cargar la orden.</Alert>
      </Box>
    )
  }

  return (
    <Box p={3} maxWidth={480} mx="auto">
      <Typography variant="h5" gutterBottom>
        Pago de Orden
      </Typography>

      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="subtitle2" color="text.secondary" gutterBottom>
            {order.orderNumber}
          </Typography>
          {order.items.map((item) => (
            <Box key={item.id} display="flex" justifyContent="space-between" mb={0.5}>
              <Typography variant="body2">
                {item.quantity}× {item.productName}
                {item.variantName ? ` (${item.variantName})` : ''}
              </Typography>
              <Typography variant="body2">${item.lineTotal.toFixed(2)}</Typography>
            </Box>
          ))}
          <Divider sx={{ my: 1 }} />
          <Box display="flex" justifyContent="space-between">
            <Typography variant="subtitle1" fontWeight="bold">Total</Typography>
            <Typography variant="subtitle1" fontWeight="bold" aria-label="total orden">
              ${order.total.toFixed(2)}
            </Typography>
          </Box>
        </CardContent>
      </Card>

      <PaymentForm
        orderTotal={order.total}
        onSubmit={handlePayment}
        isLoading={payment.isPending}
      />
    </Box>
  )
}
