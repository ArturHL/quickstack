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
import { useCategoriesQuery } from '../hooks/useCategoriesQuery'
import { useCreateCategoryMutation } from '../hooks/useCreateCategoryMutation'
import { useUpdateCategoryMutation } from '../hooks/useUpdateCategoryMutation'
import { useDeleteCategoryMutation } from '../hooks/useDeleteCategoryMutation'
import CategoryForm from './CategoryForm'
import type { CategoryResponse } from '../types/Product'

export default function CategoryList() {
  const { data: categories, isLoading, isError } = useCategoriesQuery()
  const { mutate: createCategory, isPending: isCreating } = useCreateCategoryMutation()
  const { mutate: updateCategory, isPending: isUpdating } = useUpdateCategoryMutation()
  const { mutate: deleteCategory, isPending: isDeleting } = useDeleteCategoryMutation()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<CategoryResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<CategoryResponse | null>(null)

  const handleCreate = (data: { name: string; description?: string }) => {
    createCategory(data, { onSuccess: () => setFormOpen(false) })
  }

  const handleEdit = (data: { name: string; description?: string }) => {
    if (!editTarget) return
    updateCategory({ id: editTarget.id, body: data }, { onSuccess: () => setEditTarget(null) })
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteCategory(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
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
        <Typography color="error">Error al cargar categorías.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Categorías</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => setFormOpen(true)}
        >
          Nueva Categoría
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Descripción</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {categories?.length === 0 && (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  <Typography color="text.secondary" py={2}>No hay categorías</Typography>
                </TableCell>
              </TableRow>
            )}
            {categories?.map((cat) => (
              <TableRow key={cat.id}>
                <TableCell>{cat.name}</TableCell>
                <TableCell>{cat.description ?? '—'}</TableCell>
                <TableCell>
                  <Chip
                    label={cat.isActive ? 'Activa' : 'Inactiva'}
                    color={cat.isActive ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => setEditTarget(cat)}
                      aria-label={`editar ${cat.name}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Eliminar">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteTarget(cat)}
                      aria-label={`eliminar ${cat.name}`}
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

      <CategoryForm
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreate}
        isPending={isCreating}
      />

      <CategoryForm
        key={editTarget?.id ?? ''}
        open={!!editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        isPending={isUpdating}
        initial={editTarget}
      />

      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar categoría?</DialogTitle>
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
