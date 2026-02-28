import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  List,
  Snackbar,
  Typography,
} from '@mui/material'
import { ShoppingCartOutlined } from '@mui/icons-material'
import { useCartStore, selectSubtotal, selectTax, selectTotal } from '../stores/cartStore'
import { useBranchStore } from '../stores/branchStore'
import { usePosStore } from '../stores/posStore'
import { useCreateOrderMutation } from '../hooks/useCreateOrderMutation'
import { useSubmitOrderMutation } from '../hooks/useSubmitOrderMutation'
import { useMarkReadyMutation } from '../hooks/useMarkReadyMutation'
import { buildOrderRequest } from '../utils/orderUtils'
import CartItemComponent from './CartItem'

export default function Cart() {
  const navigate = useNavigate()
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const items = useCartStore((s) => s.items)
  const serviceType = useCartStore((s) => s.serviceType)
  const tableId = useCartStore((s) => s.tableId)
  const customerId = useCartStore((s) => s.customerId)
  const clearCart = useCartStore((s) => s.clearCart)
  const updateQuantity = useCartStore((s) => s.updateQuantity)
  const removeItem = useCartStore((s) => s.removeItem)
  const subtotal = useCartStore(selectSubtotal)
  const tax = useCartStore(selectTax)
  const total = useCartStore(selectTotal)

  const activeBranchId = useBranchStore((s) => s.activeBranchId)
  const setCurrentOrderId = usePosStore((s) => s.setCurrentOrderId)

  const createOrder = useCreateOrderMutation()
  const submitOrder = useSubmitOrderMutation()
  const markReady = useMarkReadyMutation()

  const isSending = createOrder.isPending || submitOrder.isPending || markReady.isPending

  const handleEnviarOrden = async () => {
    if (!serviceType || !activeBranchId) return

    try {
      const request = buildOrderRequest(
        { items, serviceType, tableId, customerId },
        activeBranchId
      )

      const order = await createOrder.mutateAsync(request)
      const submitted = await submitOrder.mutateAsync(order.id)

      const needsAutoReady = serviceType === 'COUNTER' || serviceType === 'TAKEOUT'

      if (needsAutoReady) {
        await markReady.mutateAsync(submitted.id)
        setCurrentOrderId(submitted.id)
        navigate('/pos/payment')
      } else {
        // DINE_IN or DELIVERY: order stays IN_PROGRESS, cashier marks ready manually
        setCurrentOrderId(order.id)
        clearCart()
        navigate('/orders')
      }
    } catch {
      setErrorMsg('Error al enviar la orden. Intente de nuevo.')
    }
  }

  if (items.length === 0) {
    return (
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        p={6}
        gap={2}
      >
        <ShoppingCartOutlined sx={{ fontSize: 80, color: 'text.disabled' }} />
        <Typography variant="h6" color="text.secondary">
          Tu carrito está vacío
        </Typography>
        <Button variant="contained" onClick={() => navigate('/pos/catalog')}>
          Ir al Catálogo
        </Button>
      </Box>
    )
  }

  return (
    <Box>
      <List disablePadding>
        {items.map((item, i) => (
          <CartItemComponent
            key={i}
            item={item}
            index={i}
            onUpdateQty={updateQuantity}
            onRemove={removeItem}
          />
        ))}
      </List>

      <Divider sx={{ my: 2 }} />

      <Card variant="outlined" sx={{ mx: 2 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" mb={1}>
            <Typography>Subtotal</Typography>
            <Typography aria-label="subtotal">${subtotal.toFixed(2)}</Typography>
          </Box>
          <Box display="flex" justifyContent="space-between" mb={1}>
            <Typography>IVA (16%)</Typography>
            <Typography aria-label="impuesto">${tax.toFixed(2)}</Typography>
          </Box>
          <Divider sx={{ my: 1 }} />
          <Box display="flex" justifyContent="space-between">
            <Typography variant="h6">Total</Typography>
            <Typography variant="h6" aria-label="total">
              ${total.toFixed(2)}
            </Typography>
          </Box>
        </CardContent>
      </Card>

      <Box display="flex" gap={2} p={2}>
        <Button variant="outlined" color="error" onClick={clearCart} fullWidth>
          Limpiar Carrito
        </Button>
        <Button variant="outlined" onClick={() => navigate('/pos/new')} fullWidth>
          Continuar
        </Button>
      </Box>

      <Box px={2} pb={2}>
        <Button
          variant="contained"
          size="large"
          fullWidth
          disabled={isSending || !serviceType}
          onClick={handleEnviarOrden}
          startIcon={isSending ? <CircularProgress size={18} color="inherit" /> : undefined}
        >
          {isSending ? 'Enviando...' : 'Enviar Orden'}
        </Button>
      </Box>

      <Snackbar
        open={!!errorMsg}
        autoHideDuration={4000}
        onClose={() => setErrorMsg(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="error" onClose={() => setErrorMsg(null)}>
          {errorMsg}
        </Alert>
      </Snackbar>
    </Box>
  )
}
