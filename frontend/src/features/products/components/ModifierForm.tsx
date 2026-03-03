import { useState } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material'
import type { ModifierResponse } from '../types/Product'

interface ModifierFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { name: string; priceAdjustment: number }) => void
  isPending: boolean
  initial?: ModifierResponse | null
}

export default function ModifierForm({ open, onClose, onSubmit, isPending, initial }: ModifierFormProps) {
  const isEdit = !!initial
  const [name, setName] = useState(initial?.name ?? '')
  const [priceAdjustment, setPriceAdjustment] = useState(String(initial?.priceAdjustment ?? 0))
  const [nameError, setNameError] = useState('')

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError('El nombre es requerido')
      return
    }
    onSubmit({ name: name.trim(), priceAdjustment: parseFloat(priceAdjustment) || 0 })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Modificador' : 'Nuevo Modificador'}</DialogTitle>
      <DialogContent>
        <TextField
          label="Nombre *"
          value={name}
          onChange={(e) => { setName(e.target.value); setNameError('') }}
          error={!!nameError}
          helperText={nameError}
          fullWidth
          margin="normal"
          inputProps={{ 'aria-label': 'nombre modificador' }}
        />
        <TextField
          label="Precio adicional"
          type="number"
          value={priceAdjustment}
          onChange={(e) => setPriceAdjustment(e.target.value)}
          fullWidth
          margin="normal"
          inputProps={{ min: 0, step: 0.01, 'aria-label': 'precio adicional' }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isEdit ? 'Guardar' : 'Crear'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
