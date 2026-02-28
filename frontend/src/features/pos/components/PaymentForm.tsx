import { useState } from 'react'
import {
  Box,
  Button,
  ButtonGroup,
  Divider,
  TextField,
  Typography,
} from '@mui/material'

interface PaymentFormProps {
  orderTotal: number
  onSubmit: (amount: number) => void
  isLoading?: boolean
}

export default function PaymentForm({ orderTotal, onSubmit, isLoading = false }: PaymentFormProps) {
  const [amountInput, setAmountInput] = useState('')

  const amount = parseFloat(amountInput) || 0
  const change = amount - orderTotal
  const isValid = amount >= orderTotal

  const setQuickAmount = (value: number) => {
    setAmountInput(String(value))
  }

  return (
    <Box display="flex" flexDirection="column" gap={2}>
      <Typography variant="h6">Monto a Cobrar</Typography>

      <Box display="flex" justifyContent="space-between">
        <Typography>Total de la orden</Typography>
        <Typography aria-label="total de orden">${orderTotal.toFixed(2)}</Typography>
      </Box>

      <TextField
        label="Monto Recibido"
        type="number"
        inputProps={{ min: 0, step: '0.01', 'aria-label': 'monto recibido' }}
        value={amountInput}
        onChange={(e) => setAmountInput(e.target.value)}
        error={amountInput !== '' && !isValid}
        helperText={amountInput !== '' && !isValid ? 'El monto debe ser mayor o igual al total' : ''}
        fullWidth
      />

      <ButtonGroup variant="outlined" fullWidth aria-label="montos rápidos">
        <Button onClick={() => setQuickAmount(orderTotal)} aria-label="exacto">
          Exacto
        </Button>
        <Button onClick={() => setQuickAmount(100)} aria-label="100 pesos">
          $100
        </Button>
        <Button onClick={() => setQuickAmount(200)} aria-label="200 pesos">
          $200
        </Button>
        <Button onClick={() => setQuickAmount(500)} aria-label="500 pesos">
          $500
        </Button>
      </ButtonGroup>

      {isValid && amount > 0 && (
        <Box>
          <Divider sx={{ my: 1 }} />
          <Box display="flex" justifyContent="space-between">
            <Typography variant="subtitle1">Cambio a devolver</Typography>
            <Typography variant="subtitle1" aria-label="cambio">${change.toFixed(2)}</Typography>
          </Box>
        </Box>
      )}

      <Button
        variant="contained"
        size="large"
        disabled={!isValid || isLoading}
        onClick={() => onSubmit(amount)}
        fullWidth
      >
        {isLoading ? 'Procesando...' : 'Registrar Pago'}
      </Button>
    </Box>
  )
}
