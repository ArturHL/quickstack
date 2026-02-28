import { useState } from 'react'
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputAdornment,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { Search } from '@mui/icons-material'
import { useCustomersAdminQuery } from '../hooks/useCustomersAdminQuery'
import { useUpdateCustomerMutation } from '../hooks/useUpdateCustomerMutation'
import type { CustomerResponse } from '../../pos/types/Customer'

export default function CustomerList() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [editTarget, setEditTarget] = useState<CustomerResponse | null>(null)
  const [editName, setEditName] = useState('')
  const [editPhone, setEditPhone] = useState('')
  const [editEmail, setEditEmail] = useState('')

  const { data, isLoading, isError } = useCustomersAdminQuery({ search, page, size: 20 })
  const { mutate: updateCustomer, isPending: isUpdating } = useUpdateCustomerMutation()

  const openEdit = (customer: CustomerResponse) => {
    setEditTarget(customer)
    setEditName(customer.name ?? '')
    setEditPhone(customer.phone ?? '')
    setEditEmail(customer.email ?? '')
  }

  const handleSaveEdit = () => {
    if (!editTarget) return
    updateCustomer(
      { id: editTarget.id, body: { name: editName, phone: editPhone || undefined, email: editEmail || undefined } },
      { onSuccess: () => setEditTarget(null) }
    )
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
        <Typography color="error">Error al cargar clientes.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Clientes</Typography>
      </Box>

      <Box mb={2}>
        <TextField
          size="small"
          placeholder="Buscar por nombre, teléfono o email..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 300 }}
          inputProps={{ 'aria-label': 'buscar cliente' }}
        />
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Teléfono</TableCell>
              <TableCell>Email</TableCell>
              <TableCell align="right">Pedidos</TableCell>
              <TableCell align="right">Total gastado</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography color="text.secondary" py={2}>No hay clientes</Typography>
                </TableCell>
              </TableRow>
            )}
            {data?.content.map((customer) => (
              <TableRow
                key={customer.id}
                hover
                onClick={() => openEdit(customer)}
                sx={{ cursor: 'pointer' }}
              >
                <TableCell>{customer.name ?? '—'}</TableCell>
                <TableCell>{customer.phone ?? '—'}</TableCell>
                <TableCell>{customer.email ?? '—'}</TableCell>
                <TableCell align="right">{customer.totalOrders}</TableCell>
                <TableCell align="right">${customer.totalSpent}</TableCell>
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

      {/* Edit dialog */}
      <Dialog open={!!editTarget} onClose={() => setEditTarget(null)} fullWidth maxWidth="xs">
        <DialogTitle>Editar Cliente</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Nombre"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              inputProps={{ 'aria-label': 'nombre cliente' }}
            />
            <TextField
              label="Teléfono"
              value={editPhone}
              onChange={(e) => setEditPhone(e.target.value)}
            />
            <TextField
              label="Email"
              type="email"
              value={editEmail}
              onChange={(e) => setEditEmail(e.target.value)}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditTarget(null)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSaveEdit} disabled={isUpdating}>
            Guardar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
