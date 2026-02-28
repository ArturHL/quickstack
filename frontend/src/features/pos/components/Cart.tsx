import { useNavigate } from 'react-router-dom'
import { Box, Button, Card, CardContent, Divider, List, Typography } from '@mui/material'
import { ShoppingCartOutlined } from '@mui/icons-material'
import { useCartStore, selectSubtotal, selectTax, selectTotal } from '../stores/cartStore'
import CartItemComponent from './CartItem'

export default function Cart() {
  const navigate = useNavigate()
  const items = useCartStore((s) => s.items)
  const clearCart = useCartStore((s) => s.clearCart)
  const updateQuantity = useCartStore((s) => s.updateQuantity)
  const removeItem = useCartStore((s) => s.removeItem)
  const subtotal = useCartStore(selectSubtotal)
  const tax = useCartStore(selectTax)
  const total = useCartStore(selectTotal)

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
        <Button variant="contained" onClick={() => navigate('/pos/new')} fullWidth>
          Continuar
        </Button>
      </Box>
    </Box>
  )
}
