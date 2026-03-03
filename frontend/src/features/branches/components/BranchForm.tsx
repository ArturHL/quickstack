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
  onSubmit: (data: { name: string; code: string; address?: string; city?: string; phone?: string; email?: string }) => void
  isPending?: boolean
  initial?: BranchResponse | null
}

export default function BranchForm({ open, onClose, onSubmit, isPending, initial }: BranchFormProps) {
  const [name, setName] = useState(initial?.name ?? '')
  const [code, setCode] = useState(initial?.code ?? '')
  const [address, setAddress] = useState(initial?.address ?? '')
  const [city, setCity] = useState(initial?.city ?? '')
  const [phone, setPhone] = useState(initial?.phone ?? '')
  const [email, setEmail] = useState(initial?.email ?? '')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleSubmit = () => {
    const newErrors: Record<string, string> = {}
    if (!name.trim()) newErrors.name = 'El nombre es requerido'
    if (!code.trim()) newErrors.code = 'El código es requerido'
    setErrors(newErrors)
    if (Object.keys(newErrors).length > 0) return
    onSubmit({
      name: name.trim(),
      code: code.trim(),
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
            error={!!errors.name}
            helperText={errors.name || ' '}
            inputProps={{ 'aria-label': 'nombre sucursal' }}
          />
          <TextField
            label="Código *"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            error={!!errors.code}
            helperText={errors.code || 'Identificador corto de la sucursal (ej. SUC-01)'}
            inputProps={{ 'aria-label': 'código sucursal', maxLength: 20 }}
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
