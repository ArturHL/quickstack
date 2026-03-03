import { useState } from 'react'
import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material'
import type { ModifierGroupResponse } from '../types/Product'

type SelectionType = 'SINGLE' | 'MULTIPLE'

interface ModifierGroupFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: {
    name: string
    minSelections: number
    maxSelections: number | null
    isRequired: boolean
  }) => void
  isPending: boolean
  initial?: ModifierGroupResponse | null
}

export default function ModifierGroupForm({
  open, onClose, onSubmit, isPending, initial,
}: ModifierGroupFormProps) {
  const isEdit = !!initial
  const [name, setName] = useState(initial?.name ?? '')
  const [selectionType, setSelectionType] = useState<SelectionType>(
    initial && initial.maxSelections !== 1 ? 'MULTIPLE' : 'SINGLE'
  )
  const [isRequired, setIsRequired] = useState(initial?.isRequired ?? false)
  const [minSelections, setMinSelections] = useState(String(initial?.minSelections ?? 0))
  const [nameError, setNameError] = useState('')

  const handleSubmit = () => {
    if (!name.trim()) {
      setNameError('El nombre es requerido')
      return
    }
    onSubmit({
      name: name.trim(),
      minSelections: parseInt(minSelections) || 0,
      maxSelections: selectionType === 'SINGLE' ? 1 : null,
      isRequired,
    })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Grupo' : 'Nuevo Grupo de Modificadores'}</DialogTitle>
      <DialogContent>
        <TextField
          label="Nombre *"
          value={name}
          onChange={(e) => { setName(e.target.value); setNameError('') }}
          error={!!nameError}
          helperText={nameError}
          fullWidth
          margin="normal"
          inputProps={{ 'aria-label': 'nombre grupo' }}
        />

        <FormControl fullWidth margin="normal">
          <InputLabel id="selection-type-label">Tipo de selección</InputLabel>
          <Select
            labelId="selection-type-label"
            label="Tipo de selección"
            value={selectionType}
            onChange={(e) => setSelectionType(e.target.value as SelectionType)}
            inputProps={{ 'aria-label': 'tipo de selección' }}
          >
            <MenuItem value="SINGLE">Única (máximo 1)</MenuItem>
            <MenuItem value="MULTIPLE">Múltiple</MenuItem>
          </Select>
          <FormHelperText>
            {selectionType === 'SINGLE' ? 'El cliente elige solo una opción' : 'El cliente puede elegir varias opciones'}
          </FormHelperText>
        </FormControl>

        <TextField
          label="Mínimo de selecciones"
          type="number"
          value={minSelections}
          onChange={(e) => setMinSelections(e.target.value)}
          fullWidth
          margin="normal"
          inputProps={{ min: 0, 'aria-label': 'mínimo selecciones' }}
        />

        <FormControlLabel
          control={
            <Checkbox
              checked={isRequired}
              onChange={(e) => setIsRequired(e.target.checked)}
              inputProps={{ 'aria-label': 'requerido' }}
            />
          }
          label="Requerido"
          sx={{ mt: 1 }}
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
