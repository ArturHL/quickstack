import { useState } from 'react'
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit } from '@mui/icons-material'
import { useAreasQuery } from '../hooks/useAreasQuery'
import { useCreateAreaMutation, useUpdateAreaMutation, useDeleteAreaMutation } from '../hooks/useAreaMutations'
import AreaForm from './AreaForm'
import type { AreaResponse } from '../../pos/types/Table'

interface AreaListProps {
  branchId: string
}

export default function AreaList({ branchId }: AreaListProps) {
  const { data: areas, isLoading } = useAreasQuery(branchId)
  const { mutate: createArea, isPending: isCreating } = useCreateAreaMutation()
  const { mutate: updateArea, isPending: isUpdating } = useUpdateAreaMutation()
  const { mutate: deleteArea, isPending: isDeleting } = useDeleteAreaMutation()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<AreaResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AreaResponse | null>(null)

  const handleCreate = (data: { name: string; description?: string }) => {
    createArea({ branchId, body: data }, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (data: { name: string; description?: string }) => {
    if (!editTarget) return
    updateArea({ areaId: editTarget.id, branchId, body: data }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteArea({ areaId: deleteTarget.id, branchId }, { onSuccess: () => setDeleteTarget(null) })
  }

  if (isLoading) {
    return <CircularProgress size={24} />
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
        <Typography variant="subtitle1" fontWeight="bold">Áreas</Typography>
        <Button size="small" startIcon={<Add />} onClick={() => setFormOpen(true)}>
          Nueva Área
        </Button>
      </Box>

      {areas?.length === 0 && (
        <Typography color="text.secondary" variant="body2">No hay áreas</Typography>
      )}

      <List dense>
        {areas?.map((area) => (
          <ListItem
            key={area.id}
            secondaryAction={
              <Box>
                <Tooltip title="Editar">
                  <IconButton size="small" onClick={() => setEditTarget(area)} aria-label={`editar área ${area.name}`}>
                    <Edit fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Eliminar">
                  <IconButton size="small" color="error" onClick={() => setDeleteTarget(area)} aria-label={`eliminar área ${area.name}`}>
                    <Delete fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            }
          >
            <ListItemText primary={area.name} secondary={area.description} />
          </ListItem>
        ))}
      </List>

      <AreaForm open={formOpen} onClose={() => setFormOpen(false)} onSubmit={handleCreate} isPending={isCreating} />
      <AreaForm key={editTarget?.id ?? ''} open={!!editTarget} onClose={() => setEditTarget(null)} onSubmit={handleEdit} isPending={isUpdating} initial={editTarget} />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar área?</DialogTitle>
        <DialogContent>
          <Typography>¿Eliminar &quot;{deleteTarget?.name}&quot;?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleConfirmDelete} disabled={isDeleting}>Eliminar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
