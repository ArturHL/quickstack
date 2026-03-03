import { useState, useEffect } from 'react'
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material'
import type { CategoryResponse } from '../types/Product'

interface CategoryFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { name: string; description?: string }) => void
  isPending: boolean
  initial?: CategoryResponse | null
}

export default function CategoryForm({ open, onClose, onSubmit, isPending, initial }: CategoryFormProps) {
  const isEdit = !!initial
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [nameError, setNameError] = useState('')

  useEffect(() => {
    if (open) {
      setName(initial?.name ?? '')
      setDescription(initial?.description ?? '')
      setNameError('')
    }
  }, [open, initial])

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError('El nombre es requerido')
      return
    }
    onSubmit({ name: name.trim(), description: description.trim() || undefined })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Categoría' : 'Nueva Categoría'}</DialogTitle>
      <DialogContent>
        <TextField
          label="Nombre *"
          value={name}
          onChange={(e) => { setName(e.target.value); setNameError('') }}
          error={!!nameError}
          helperText={nameError}
          fullWidth
          margin="normal"
          inputProps={{ 'aria-label': 'nombre categoría' }}
        />
        <TextField
          label="Descripción"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          fullWidth
          margin="normal"
          multiline
          rows={2}
          inputProps={{ 'aria-label': 'descripción categoría' }}
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
