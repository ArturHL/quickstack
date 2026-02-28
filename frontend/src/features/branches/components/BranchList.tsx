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
import { useBranchesQuery } from '../hooks/useBranchesQuery'
import { useCreateBranchMutation, useUpdateBranchMutation, useDeleteBranchMutation } from '../hooks/useBranchMutations'
import BranchForm from './BranchForm'
import type { BranchResponse } from '../types/Branch'

export default function BranchList() {
  const { data: branches, isLoading, isError } = useBranchesQuery()
  const { mutate: createBranch, isPending: isCreating } = useCreateBranchMutation()
  const { mutate: updateBranch, isPending: isUpdating } = useUpdateBranchMutation()
  const { mutate: deleteBranch, isPending: isDeleting } = useDeleteBranchMutation()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<BranchResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<BranchResponse | null>(null)

  const handleCreate = (data: Parameters<typeof createBranch>[0]) => {
    createBranch(data, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (data: { name: string; address?: string; city?: string; phone?: string; email?: string }) => {
    if (!editTarget) return
    updateBranch({ id: editTarget.id, body: data }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteBranch(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
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
        <Typography color="error">Error al cargar sucursales.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Sucursales</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setFormOpen(true)}
        >
          Nueva Sucursal
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Ciudad</TableCell>
              <TableCell>Teléfono</TableCell>
              <TableCell>Email</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {branches?.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography color="text.secondary" py={2}>No hay sucursales</Typography>
                </TableCell>
              </TableRow>
            )}
            {branches?.map((branch) => (
              <TableRow key={branch.id}>
                <TableCell>{branch.name}</TableCell>
                <TableCell>{branch.city ?? '—'}</TableCell>
                <TableCell>{branch.phone ?? '—'}</TableCell>
                <TableCell>{branch.email ?? '—'}</TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => setEditTarget(branch)}
                      aria-label={`editar ${branch.name}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Eliminar">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteTarget(branch)}
                      aria-label={`eliminar ${branch.name}`}
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

      {/* Create form */}
      <BranchForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreate}
        isPending={isCreating}
      />

      {/* Edit form */}
      <BranchForm
        key={editTarget?.id ?? ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        isPending={isUpdating}
        initial={editTarget}
      />

      {/* Delete confirmation */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar sucursal?</DialogTitle>
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
