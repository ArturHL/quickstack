import { useState } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit } from '@mui/icons-material'
import { useCreateModifierMutation, useUpdateModifierMutation, useDeleteModifierMutation } from '../hooks/useModifierMutations'
import ModifierForm from './ModifierForm'
import type { ModifierGroupResponse, ModifierResponse } from '../types/Product'

interface ModifierListProps {
  productId: string
  group: ModifierGroupResponse
}

export default function ModifierList({ productId, group }: ModifierListProps) {
  const { mutate: createModifier, isPending: isCreating } = useCreateModifierMutation(productId)
  const { mutate: updateModifier, isPending: isUpdating } = useUpdateModifierMutation(productId)
  const { mutate: deleteModifier, isPending: isDeleting } = useDeleteModifierMutation(productId)

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ModifierResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ModifierResponse | null>(null)

  const handleCreate = (data: { name: string; priceAdjustment: number }) => {
    createModifier(
      { groupId: group.id, body: data },
      { onSuccess: () => setFormOpen(false) }
    )
  }

  const handleEdit = (data: { name: string; priceAdjustment: number }) => {
    if (!editTarget) return
    updateModifier(
      { modifierId: editTarget.id, body: data },
      { onSuccess: () => setEditTarget(null) }
    )
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteModifier(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  return (
    <Box ml={2} mt={1}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={0.5}>
        <Typography variant="body2" color="text.secondary">
          Opciones ({group.modifiers.length})
        </Typography>
        <Button
          size="small"
          startIcon={<Add />}
          onClick={() => setFormOpen(true)}
          aria-label={`agregar opción a ${group.name}`}
        >
          Agregar opción
        </Button>
      </Box>

      {group.modifiers.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell align="right">Precio adicional</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {group.modifiers.map((mod) => (
              <TableRow key={mod.id}>
                <TableCell>{mod.name}</TableCell>
                <TableCell align="right">
                  {mod.priceAdjustment > 0 ? `+$${mod.priceAdjustment.toFixed(2)}` : '—'}
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => setEditTarget(mod)}
                      aria-label={`editar modificador ${mod.name}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Eliminar">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteTarget(mod)}
                      aria-label={`eliminar modificador ${mod.name}`}
                    >
                      <Delete fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <ModifierForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreate}
        isPending={isCreating}
      />

      <ModifierForm
        key={editTarget?.id ?? ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        isPending={isUpdating}
        initial={editTarget}
      />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar modificador?</DialogTitle>
        <DialogContent>
          <Typography>¿Eliminar &quot;{deleteTarget?.name}&quot;?</Typography>
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
