import { useState } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material'
import type { TableResponse } from '../../pos/types/Table'

interface TableFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { number: number; name?: string; capacity: number }) => void
  isPending?: boolean
  initial?: TableResponse | null
}

export default function TableForm({ open, onClose, onSubmit, isPending, initial }: TableFormProps) {
  const [number, setNumber] = useState(initial?.number != null ? String(initial.number) : '')
  const [name, setName] = useState(initial?.name ?? '')
  const [capacity, setCapacity] = useState(initial?.capacity != null ? String(initial.capacity) : '')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleSubmit = () => {
    const newErrors: Record<string, string> = {}
    const num = parseInt(number)
    if (isNaN(num) || num < 1) newErrors.number = 'Número inválido'
    const cap = parseInt(capacity)
    if (isNaN(cap) || cap < 1) newErrors.capacity = 'Capacidad inválida'
    setErrors(newErrors)
    if (Object.keys(newErrors).length > 0) return
    onSubmit({ number: num, name: name.trim() || undefined, capacity: cap })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{initial ? 'Editar Mesa' : 'Nueva Mesa'}</DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" gap={2} pt={1}>
          <TextField
            label="Número de mesa *"
            type="number"
            value={number}
            onChange={(e) => setNumber(e.target.value)}
            error={!!errors.number}
            helperText={errors.number || ' '}
            inputProps={{ min: 1, 'aria-label': 'número de mesa' }}
          />
          <TextField
            label="Nombre (opcional)"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <TextField
            label="Capacidad *"
            type="number"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value)}
            error={!!errors.capacity}
            helperText={errors.capacity || ' '}
            inputProps={{ min: 1, 'aria-label': 'capacidad' }}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {initial ? 'Guardar' : 'Crear'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
