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
  InputAdornment,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete, Edit, Search } from '@mui/icons-material'
import { useUsersAdminQuery } from '../hooks/useUsersAdminQuery'
import { useCreateUserMutation } from '../hooks/useCreateUserMutation'
import { useUpdateUserMutation } from '../hooks/useUpdateUserMutation'
import { useDeleteUserMutation } from '../hooks/useDeleteUserMutation'
import { ROLE_OPTIONS, type UserResponse } from '../api/userApi'
import { useAuthStore } from '../../../stores/authStore'

const ROLE_LABELS: Record<string, string> = {
  OWNER: 'Dueño',
  CASHIER: 'Cajero',
  KITCHEN: 'Cocina',
}

export default function UserList() {
  const [search, setSearch] = useState('')
  const [page] = useState(0)

  // Create dialog state
  const [createOpen, setCreateOpen] = useState(false)
  const [newEmail, setNewEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [newFullName, setNewFullName] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [newRoleId, setNewRoleId] = useState<string>(ROLE_OPTIONS[0].id)

  // Edit dialog state
  const [editTarget, setEditTarget] = useState<UserResponse | null>(null)
  const [editFullName, setEditFullName] = useState('')
  const [editPhone, setEditPhone] = useState('')
  const [editRoleId, setEditRoleId] = useState('')

  // Delete dialog state
  const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null)

  const currentUserId = useAuthStore((s) => s.user?.id)

  const { data, isLoading, isError } = useUsersAdminQuery({ search, page, size: 20 })
  const { mutate: createUser, isPending: isCreating } = useCreateUserMutation()
  const { mutate: updateUser, isPending: isUpdating } = useUpdateUserMutation()
  const { mutate: deleteUser, isPending: isDeleting } = useDeleteUserMutation()

  const handleOpenCreate = () => {
    setNewEmail('')
    setNewPassword('')
    setNewFullName('')
    setNewPhone('')
    setNewRoleId(ROLE_OPTIONS[0].id)
    setCreateOpen(true)
  }

  const handleCreate = () => {
    createUser(
      {
        email: newEmail.trim(),
        fullName: newFullName.trim(),
        password: newPassword,
        roleId: newRoleId,
        phone: newPhone.trim() || undefined,
      },
      { onSuccess: () => setCreateOpen(false) }
    )
  }

  const handleOpenEdit = (user: UserResponse) => {
    setEditTarget(user)
    setEditFullName(user.fullName)
    setEditPhone(user.phone ?? '')
    setEditRoleId(user.roleId)
  }

  const handleSaveEdit = () => {
    if (!editTarget) return
    updateUser(
      {
        id: editTarget.id,
        body: {
          fullName: editFullName || undefined,
          phone: editPhone || undefined,
          roleId: editRoleId || undefined,
        },
      },
      { onSuccess: () => setEditTarget(null) }
    )
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget) return
    deleteUser(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) })
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
        <Typography color="error">Error al cargar usuarios.</Typography>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h5">Usuarios</Typography>
        <Button variant="contained" startIcon={<Add />} onClick={handleOpenCreate}>
          Nuevo Usuario
        </Button>
      </Box>

      <Box mb={2}>
        <TextField
          size="small"
          placeholder="Buscar usuario..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search fontSize="small" />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 300 }}
          inputProps={{ 'aria-label': 'buscar usuario' }}
        />
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Rol</TableCell>
              <TableCell>Estado</TableCell>
              <TableCell align="center">Acciones</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data?.content.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography color="text.secondary" py={2}>No hay usuarios</Typography>
                </TableCell>
              </TableRow>
            )}
            {data?.content.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{user.fullName}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{ROLE_LABELS[user.roleCode] ?? user.roleCode}</TableCell>
                <TableCell>
                  <Chip
                    label={user.isActive ? 'Activo' : 'Inactivo'}
                    color={user.isActive ? 'success' : 'default'}
                    size="small"
                  />
                </TableCell>
                <TableCell align="center">
                  <Tooltip title="Editar">
                    <IconButton
                      size="small"
                      onClick={() => handleOpenEdit(user)}
                      aria-label={`editar ${user.fullName}`}
                    >
                      <Edit fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title={user.id === currentUserId ? 'No puedes eliminar tu propia cuenta' : 'Eliminar'}>
                    <span>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => setDeleteTarget(user)}
                        disabled={user.id === currentUserId}
                        aria-label={`eliminar ${user.fullName}`}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Create dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Nuevo Usuario</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Email *"
              type="email"
              value={newEmail}
              onChange={(e) => setNewEmail(e.target.value)}
              inputProps={{ 'aria-label': 'email nuevo usuario' }}
            />
            <TextField
              label="Contraseña * (mínimo 12 caracteres)"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              inputProps={{ 'aria-label': 'contraseña nuevo usuario' }}
            />
            <TextField
              label="Nombre completo *"
              value={newFullName}
              onChange={(e) => setNewFullName(e.target.value)}
              inputProps={{ 'aria-label': 'nombre nuevo usuario' }}
            />
            <TextField
              label="Teléfono"
              value={newPhone}
              onChange={(e) => setNewPhone(e.target.value)}
              inputProps={{ 'aria-label': 'teléfono nuevo usuario' }}
            />
            <Select
              value={newRoleId}
              onChange={(e) => setNewRoleId(e.target.value)}
              inputProps={{ 'aria-label': 'rol nuevo usuario' }}
            >
              {ROLE_OPTIONS.map((r) => (
                <MenuItem key={r.id} value={r.id}>{r.label}</MenuItem>
              ))}
            </Select>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={handleCreate} disabled={isCreating}>
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit dialog */}
      <Dialog open={!!editTarget} onClose={() => setEditTarget(null)} fullWidth maxWidth="xs">
        <DialogTitle>Editar Usuario</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Email"
              value={editTarget?.email ?? ''}
              disabled
              inputProps={{ 'aria-label': 'email usuario (solo lectura)' }}
            />
            <TextField
              label="Nombre completo"
              value={editFullName}
              onChange={(e) => setEditFullName(e.target.value)}
              inputProps={{ 'aria-label': 'nombre usuario' }}
            />
            <TextField
              label="Teléfono"
              value={editPhone}
              onChange={(e) => setEditPhone(e.target.value)}
              inputProps={{ 'aria-label': 'teléfono usuario' }}
            />
            <Select
              value={editRoleId}
              onChange={(e) => setEditRoleId(e.target.value)}
              inputProps={{ 'aria-label': 'rol usuario' }}
            >
              {ROLE_OPTIONS.map((r) => (
                <MenuItem key={r.id} value={r.id}>{r.label}</MenuItem>
              ))}
            </Select>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditTarget(null)}>Cancelar</Button>
          <Button variant="contained" onClick={handleSaveEdit} disabled={isUpdating}>
            Guardar
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete confirmation */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>¿Eliminar usuario?</DialogTitle>
        <DialogContent>
          <Typography>
            ¿Eliminar a &quot;{deleteTarget?.fullName}&quot;? El usuario perderá acceso al sistema.
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
