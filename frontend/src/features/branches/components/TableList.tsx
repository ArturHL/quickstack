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
  List,
  ListItem,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit } from '@mui/icons-material'
import { useTablesAdminQuery } from '../hooks/useTablesAdminQuery'
import { useCreateTableMutation, useUpdateTableMutation, useDeleteTableMutation } from '../hooks/useTableMutations'
import TableForm from './TableForm'
import type { TableResponse } from '../../pos/types/Table'

const STATUS_COLORS = {
  AVAILABLE: 'success',
  OCCUPIED: 'error',
  RESERVED: 'warning',
  MAINTENANCE: 'default',
} as const

const STATUS_LABELS = {
  AVAILABLE: 'Disponible',
  OCCUPIED: 'Ocupada',
  RESERVED: 'Reservada',
  MAINTENANCE: 'Mantenimiento',
}

interface TableListProps {
  areaId: string
}

export default function TableList({ areaId }: TableListProps) {
  const { data: tables, isLoading } = useTablesAdminQuery(areaId)
  const { mutate: createTable, isPending: isCreating } = useCreateTableMutation()
  const { mutate: updateTable, isPending: isUpdating } = useUpdateTableMutation()
  const { mutate: deleteTable, isPending: isDeleting } = useDeleteTableMutation()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<TableResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<TableResponse | null>(null)

  const handleCreate = (data: { number: number; name?: string; capacity: number }) => {
    createTable({ areaId, body: data }, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (data: { number: number; name?: string; capacity: number }) => {
    if (!editTarget) return
    updateTable({ tableId: editTarget.id, areaId, body: data }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteTable({ tableId: deleteTarget.id, areaId }, { onSuccess: () => setDeleteTarget(null) })
  }

  if (isLoading) {
    return <CircularProgress size={24} />
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
        <Typography variant="subtitle1" fontWeight="bold">Mesas</Typography>
        <Button size="small" startIcon={<Add />} onClick={() => setFormOpen(true)}>
          Nueva Mesa
        </Button>
      </Box>

      {tables?.length === 0 && (
        <Typography color="text.secondary" variant="body2">No hay mesas</Typography>
      )}

      <List dense>
        {tables?.map((table) => (
          <ListItem
            key={table.id}
            secondaryAction={
              <Box>
                <Tooltip title="Editar">
                  <IconButton size="small" onClick={() => setEditTarget(table)} aria-label={`editar mesa ${table.number}`}>
                    <Edit fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Eliminar">
                  <IconButton size="small" color="error" onClick={() => setDeleteTarget(table)} aria-label={`eliminar mesa ${table.number}`}>
                    <Delete fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            }
          >
            <ListItemText
              primary={`Mesa ${table.number}${table.name ? ` — ${table.name}` : ''}`}
              secondary={`Capacidad: ${table.capacity}`}
            />
            <Chip
              label={STATUS_LABELS[table.status]}
              color={STATUS_COLORS[table.status]}
              size="small"
              sx={{ mr: 6 }}
            />
          </ListItem>
        ))}
      </List>

      <TableForm open={formOpen} onClose={() => setFormOpen(false)} onSubmit={handleCreate} isPending={isCreating} />
      <TableForm key={editTarget?.id ?? ''} open={!!editTarget} onClose={() => setEditTarget(null)} onSubmit={handleEdit} isPending={isUpdating} initial={editTarget} />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar mesa?</DialogTitle>
        <DialogContent>
          <Typography>¿Eliminar mesa {deleteTarget?.number}?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={handleConfirmDelete} disabled={isDeleting}>Eliminar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
