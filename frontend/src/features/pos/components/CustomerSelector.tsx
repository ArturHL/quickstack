import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  List,
  ListItemButton,
  ListItemText,
  TextField,
  Typography,
} from '@mui/material'
import { PersonAdd } from '@mui/icons-material'
import { useCustomersQuery } from '../hooks/useCustomersQuery'
import { useCreateCustomerMutation } from '../hooks/useCreateCustomerMutation'
import { useCartStore } from '../stores/cartStore'

export default function CustomerSelector() {
  const navigate = useNavigate()
  const serviceType = useCartStore((s) => s.serviceType)
  const setServiceDetails = useCartStore((s) => s.setServiceDetails)

  const [searchInput, setSearchInput] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchInput), 300)
    return () => clearTimeout(timer)
  }, [searchInput])

  const { data: page, isLoading, isError } = useCustomersQuery(debouncedSearch)
  const createMutation = useCreateCustomerMutation()

  const handleSelect = (customerId: string) => {
    setServiceDetails(serviceType ?? 'DELIVERY', undefined, customerId)
    navigate('/pos/catalog')
  }

  const handleCreate = () => {
    if (!newCustomerName && !newCustomerPhone) return
    createMutation.mutate(
      {
        name: newCustomerName || undefined,
        phone: newCustomerPhone || undefined,
      },
      {
        onSuccess: (customer) => {
          setCreateDialogOpen(false)
          setNewCustomerName('')
          setNewCustomerPhone('')
          handleSelect(customer.id)
        },
      }
    )
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Seleccionar Cliente
      </Typography>

      <TextField
        label="Buscar cliente"
        value={searchInput}
        onChange={(e) => setSearchInput(e.target.value)}
        fullWidth
        sx={{ mb: 2 }}
        placeholder="Nombre, teléfono o email..."
        inputProps={{ 'aria-label': 'Buscar cliente' }}
      />

      <Button
        variant="outlined"
        startIcon={<PersonAdd />}
        onClick={() => setCreateDialogOpen(true)}
        sx={{ mb: 2 }}
      >
        Crear Cliente Nuevo
      </Button>

      {isLoading && (
        <Box display="flex" justifyContent="center" p={2}>
          <CircularProgress size={24} aria-label="Buscando clientes" />
        </Box>
      )}

      {isError && <Alert severity="error">Error al buscar clientes</Alert>}

      {page && (
        <List>
          {page.content.length === 0 ? (
            <Box p={2}>
              <Typography color="text.secondary">No se encontraron clientes</Typography>
            </Box>
          ) : (
            page.content.map((customer) => (
              <ListItemButton key={customer.id} onClick={() => handleSelect(customer.id)}>
                <ListItemText
                  primary={customer.name ?? customer.phone ?? customer.email}
                  secondary={[customer.phone, customer.email].filter(Boolean).join(' • ')}
                />
              </ListItemButton>
            ))
          )}
        </List>
      )}

      <Dialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Crear Cliente Nuevo</DialogTitle>
        <DialogContent>
          <TextField
            label="Nombre"
            value={newCustomerName}
            onChange={(e) => setNewCustomerName(e.target.value)}
            fullWidth
            sx={{ mt: 1, mb: 2 }}
            inputProps={{ 'aria-label': 'nombre del cliente' }}
          />
          <TextField
            label="Teléfono"
            value={newCustomerPhone}
            onChange={(e) => setNewCustomerPhone(e.target.value)}
            fullWidth
            inputProps={{ 'aria-label': 'teléfono del cliente' }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>Cancelar</Button>
          <Button
            onClick={handleCreate}
            variant="contained"
            disabled={!newCustomerName && !newCustomerPhone}
          >
            Crear
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
