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
import type { AreaResponse } from '../../pos/types/Table'

interface AreaFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { name: string; description?: string }) => void
  isPending?: boolean
  initial?: AreaResponse | null
}

export default function AreaForm({ open, onClose, onSubmit, isPending, initial }: AreaFormProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [nameError, setNameError] = useState('')

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError('El nombre es requerido')
      return
    }
    setNameError('')
    onSubmit({ name: name.trim(), description: description.trim() || undefined })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{initial ? 'Editar Área' : 'Nueva Área'}</DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" gap={2} pt={1}>
          <TextField
            label="Nombre *"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={!!nameError}
            helperText={nameError || ' '}
            inputProps={{ 'aria-label': 'nombre área' }}
          />
          <TextField
            label="Descripción"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
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
