import { useState } from 'react'
import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit } from '@mui/icons-material'
import { useCombosQuery } from '../hooks/useCombosQuery'
import { useCreateComboMutation, useUpdateComboMutation, useDeleteComboMutation } from '../hooks/useComboMutations'
import ComboForm from './ComboForm'
import type { ComboResponse } from '../types/Product'

export default function ComboList() {
  const { data, isLoading, isError } = useCombosQuery()
  const combos = data ?? []
  const { mutate: createCombo, isPending: isCreating } = useCreateComboMutation()
  const { mutate: updateCombo, isPending: isUpdating } = useUpdateComboMutation()
  const { mutate: deleteCombo, isPending: isDeleting } = useDeleteComboMutation()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ComboResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ComboResponse | null>(null)

  const handleCreate = (body: Parameters<typeof createCombo>[0]) => {
    createCombo(body, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (body: Parameters<typeof createCombo>[0]) => {
    if (!editTarget) return
    updateCombo({ id: editTarget.id, body }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteCombo(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    )
  }

  if (isError) {
    return (
      <Box p={4}>
        <Typography color="error">Error al cargar combos.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Combos</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={() => setFormOpen(true)}>
          Nuevo Combo
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell align="right">Precio</TableCell>
              <TableCell>Componentes</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {combos.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography color="text.secondary" py={2}>No hay combos</Typography>
                </TableCell>
              </TableRow>
            )}
            {combos.map((combo) => (
              <TableRow key={combo.id}>
                <TableCell>{combo.name}</TableCell>
                <TableCell>{combo.description ?? '—'}</TableCell>
                <TableCell align="right">${combo.price.toFixed(2)}</TableCell>
                <TableCell>
                  {combo.items.length > 0
                    ? combo.items.map((i) => `${i.productName} ×${i.quantity}`).join(', ')
                    : '—'}
                </TableCell>
                <TableCell>
                  <Chip
                    label={combo.isActive ? 'Activo' : 'Inactivo'}
                    color={combo.isActive ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => setEditTarget(combo)}
                      aria-label={`editar ${combo.name}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Eliminar">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteTarget(combo)}
                      aria-label={`eliminar ${combo.name}`}
                    >
                      <Delete fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <ComboForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreate}
        isPending={isCreating}
      />

      <ComboForm
        key={editTarget?.id ?? ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        isPending={isUpdating}
        initial={editTarget}
      />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar combo?</DialogTitle>
        <DialogContent>
          <Typography>
            ¿Eliminar &quot;{deleteTarget?.name}&quot;? Esta acción no se puede deshacer.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleConfirmDelete} disabled={isDeleting}>
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
