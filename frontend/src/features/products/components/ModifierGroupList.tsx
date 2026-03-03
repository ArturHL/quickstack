import { useState } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit, ExpandMore } from '@mui/icons-material'
import { useModifierGroupsQuery } from '../hooks/useModifierGroupsQuery'
import {
  useCreateModifierGroupMutation,
  useUpdateModifierGroupMutation,
  useDeleteModifierGroupMutation,
} from '../hooks/useModifierGroupMutations'
import ModifierGroupForm from './ModifierGroupForm'
import ModifierList from './ModifierList'
import type { ModifierGroupResponse } from '../types/Product'

interface ModifierGroupListProps {
  productId: string
}

export default function ModifierGroupList({ productId }: ModifierGroupListProps) {
  const { data: groups, isLoading, isError } = useModifierGroupsQuery(productId)
  const { mutate: createGroup, isPending: isCreating } = useCreateModifierGroupMutation(productId)
  const { mutate: updateGroup, isPending: isUpdating } = useUpdateModifierGroupMutation(productId)
  const { mutate: deleteGroup, isPending: isDeleting } = useDeleteModifierGroupMutation(productId)

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ModifierGroupResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ModifierGroupResponse | null>(null)

  const handleCreate = (data: Parameters<typeof createGroup>[0]) => {
    createGroup(data, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (data: Parameters<typeof createGroup>[0]) => {
    if (!editTarget) return
    updateGroup({ groupId: editTarget.id, body: data }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteGroup(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={2}>
        <CircularProgress size={24} />
      </Box>
    )
  }

  if (isError) {
    return (
      <Typography color="error" variant="body2">
        Error al cargar grupos de modificadores.
      </Typography>
    )
  }

  return (
    <Box mt={4}>
      <Divider sx={{ mb: 2 }} />
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Grupos de Modificadores</Typography>
        <Button
          variant="outlined"
          startIcon={<Add />}
          onClick={() => setFormOpen(true)}
        >
          Agregar Grupo
        </Button>
      </Box>

      {groups?.length === 0 && (
        <Typography color="text.secondary" variant="body2">
          Sin grupos de modificadores. Agrega uno para personalizar este producto.
        </Typography>
      )}

      {groups?.map((group) => (
        <Accordion key={group.id} disableGutters sx={{ mb: 1 }}>
          <AccordionSummary expandIcon={<ExpandMore />}>
            <Box display="flex" alignItems="center" gap={1} flex={1} mr={1}>
              <Typography sx={{ flex: 1 }}>{group.name}</Typography>
              <Chip
                label={group.maxSelections === 1 ? 'Única' : 'Múltiple'}
                size="small"
                variant="outlined"
              />
              {group.isRequired && (
                <Chip label="Requerido" size="small" color="warning" />
              )}
              <Tooltip title="Editar grupo">
                <IconButton
                  size="small"
                  onClick={(e) => { e.stopPropagation(); setEditTarget(group) }}
                  aria-label={`editar grupo ${group.name}`}
                >
                  <Edit fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Eliminar grupo">
                <IconButton
                  size="small"
                  color="error"
                  onClick={(e) => { e.stopPropagation(); setDeleteTarget(group) }}
                  aria-label={`eliminar grupo ${group.name}`}
                >
                  <Delete fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          </AccordionSummary>
          <AccordionDetails>
            <ModifierList productId={productId} group={group} />
          </AccordionDetails>
        </Accordion>
      ))}

      <ModifierGroupForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreate}
        isPending={isCreating}
      />

      <ModifierGroupForm
        key={editTarget?.id ?? ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        isPending={isUpdating}
        initial={editTarget}
      />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar grupo?</DialogTitle>
        <DialogContent>
          <Typography>
            ¿Eliminar el grupo &quot;{deleteTarget?.name}&quot; y todos sus modificadores?
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
