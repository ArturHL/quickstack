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
import type { BranchResponse } from '../types/Branch'

interface BranchFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: { name: string; address?: string; city?: string; phone?: string; email?: string }) => void
  isPending?: boolean
  initial?: BranchResponse | null
}

export default function BranchForm({ open, onClose, onSubmit, isPending, initial }: BranchFormProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [address, setAddress] = useState(initial?.address ?? '')
  const [city, setCity] = useState(initial?.city ?? '')
  const [phone, setPhone] = useState(initial?.phone ?? '')
  const [email, setEmail] = useState(initial?.email ?? '')
  const [nameError, setNameError] = useState('')

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError('El nombre es requerido')
      return
    }
    setNameError('')
    onSubmit({
      name: name.trim(),
      address: address.trim() || undefined,
      city: city.trim() || undefined,
      phone: phone.trim() || undefined,
      email: email.trim() || undefined,
    })
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{initial ? 'Editar Sucursal' : 'Nueva Sucursal'}</DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" gap={2} pt={1}>
          <TextField
            label="Nombre *"
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={!!nameError}
            helperText={nameError || ' '}
            inputProps={{ 'aria-label': 'nombre sucursal' }}
          />
          <TextField
            label="Dirección"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
          <TextField
            label="Ciudad"
            value={city}
            onChange={(e) => setCity(e.target.value)}
          />
          <TextField
            label="Teléfono"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
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
