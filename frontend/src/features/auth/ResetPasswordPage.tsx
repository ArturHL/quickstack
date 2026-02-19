import { useState } from 'react'
import { Link as RouterLink, Navigate, useSearchParams } from 'react-router-dom'
import type { AxiosError } from 'axios'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Link from '@mui/material/Link'
import IconButton from '@mui/material/IconButton'
import InputAdornment from '@mui/material/InputAdornment'
import Visibility from '@mui/icons-material/Visibility'
import VisibilityOff from '@mui/icons-material/VisibilityOff'
import { useResetPassword } from '../../hooks/useAuthQuery'
import type { ApiError } from '../../types/auth'

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [passwordMismatch, setPasswordMismatch] = useState(false)
  const [success, setSuccess] = useState(false)

  const { mutate: resetPassword, isPending, error } = useResetPassword()

  if (!token) return <Navigate to="/forgot-password" replace />

  const axiosError = error as AxiosError<ApiError> | null
  const getErrorMessage = () => {
    if (!axiosError) return null
    if (axiosError.response?.status === 400) {
      return 'El enlace de recuperación ha expirado. Solicita uno nuevo.'
    }
    return 'Ocurrió un error. Intenta de nuevo.'
  }
  const errorMessage = getErrorMessage()

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setPasswordMismatch(true)
      return
    }
    setPasswordMismatch(false)
    resetPassword(
      { token, newPassword },
      { onSuccess: () => setSuccess(true) }
    )
  }

  if (success) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="background.default">
        <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }}>
          <CardContent sx={{ p: 4 }}>
            <Alert severity="success" sx={{ mb: 3 }}>
              Contraseña actualizada correctamente.
            </Alert>
            <Link component={RouterLink} to="/login" variant="body1">
              Ir al login
            </Link>
          </CardContent>
        </Card>
      </Box>
    )
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="background.default">
      <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} textAlign="center" mb={1}>
            Nueva contraseña
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" mb={3}>
            Elige una contraseña segura
          </Typography>

          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }} role="alert">
              {errorMessage}{' '}
              {axiosError?.response?.status === 400 && (
                <Link component={RouterLink} to="/forgot-password">
                  Solicitar nuevo enlace
                </Link>
              )}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              label="Nueva contraseña"
              type={showPassword ? 'text' : 'password'}
              required
              fullWidth
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              sx={{ mb: 2 }}
              inputProps={{ 'data-testid': 'new-password-input' }}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword((p) => !p)}
                      edge="end"
                      aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              label="Confirmar contraseña"
              type="password"
              required
              fullWidth
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value)
                if (passwordMismatch) setPasswordMismatch(false)
              }}
              error={passwordMismatch}
              helperText={passwordMismatch ? 'Las contraseñas no coinciden' : ''}
              sx={{ mb: 3 }}
              inputProps={{ 'data-testid': 'confirm-new-password-input' }}
            />

            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={isPending}
              startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : null}
            >
              {isPending ? 'Guardando...' : 'Guardar contraseña'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default ResetPasswordPage
