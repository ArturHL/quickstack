import { useState } from 'react'
import {
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material'
import { Add, Delete, Edit, Search } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useProductsQuery } from '../hooks/useProductsQuery'
import { useCategoriesQuery } from '../hooks/useCategoriesQuery'
import { useDeleteProductMutation } from '../hooks/useDeleteProductMutation'
import type { ProductResponse } from '../types/Product'

export default function ProductList() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState<ProductResponse | null>(null)

  const { data, isLoading, isError } = useProductsQuery({ search, categoryId: categoryId || undefined, page, size: 20 })
  const { data: categories } = useCategoriesQuery()
  const { mutate: deleteProduct, isPending: isDeleting } = useDeleteProductMutation()

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteProduct(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
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
        <Typography color="error">Error al cargar productos.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Productos</Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={() => navigate('/admin/products/new')}
        >
          Nuevo Producto
        </Button>
      </Box>

      {/* Filters */}
      <Box display="flex" gap={2} mb={2} flexWrap="wrap">
        <TextField
          size="small"
          placeholder="Buscar producto..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 220 }}
          inputProps={{ 'aria-label': 'buscar producto' }}
        />
        <Select
          size="small"
          displayEmpty
          value={categoryId}
          onChange={(e) => { setCategoryId(e.target.value); setPage(0) }}
          sx={{ minWidth: 160 }}
          inputProps={{ 'aria-label': 'filtrar por categoría' }}
        >
          <MenuItem value="">Todas las categorías</MenuItem>
          {categories?.map((cat) => (
            <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>
          ))}
        </Select>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Categoría</TableCell>
              <TableCell align="right">Precio</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography color="text.secondary" py={2}>No hay productos</Typography>
                </TableCell>
              </TableRow>
            )}
            {data?.content.map((product) => (
              <TableRow key={product.id}>
                <TableCell>{product.name}</TableCell>
                <TableCell>{product.categoryName ?? '—'}</TableCell>
                <TableCell align="right">${product.basePrice.toFixed(2)}</TableCell>
                <TableCell>{product.productType}</TableCell>
                <TableCell>
                  <Chip
                    label={product.isActive ? 'Activo' : 'Inactivo'}
                    color={product.isActive ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/admin/products/${product.id}/edit`)}
                      aria-label={`editar ${product.name}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Eliminar">
                    <IconButton
                      size="small"
                      color="error"
                      onClick={() => setDeleteTarget(product)}
                      aria-label={`eliminar ${product.name}`}
                    >
                      <Delete fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={data?.totalElements ?? 0}
          page={page}
          onPageChange={(_e, p) => setPage(p)}
          rowsPerPage={20}
          rowsPerPageOptions={[20]}
        />
      </TableContainer>

      {/* Delete confirmation dialog */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar producto?</DialogTitle>
        <DialogContent>
          <Typography>
            ¿Estás seguro de eliminar &quot;{deleteTarget?.name}&quot;? Esta acción no se puede deshacer.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancelar</Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleConfirmDelete}
            disabled={isDeleting}
          >
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
