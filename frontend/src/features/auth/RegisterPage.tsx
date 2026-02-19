import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import type { AxiosError } from 'axios'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import IconButton from '@mui/material/IconButton'
import InputAdornment from '@mui/material/InputAdornment'
import CircularProgress from '@mui/material/CircularProgress'
import Link from '@mui/material/Link'
import LinearProgress from '@mui/material/LinearProgress'
import Visibility from '@mui/icons-material/Visibility'
import VisibilityOff from '@mui/icons-material/VisibilityOff'
import { useRegister } from '../../hooks/useAuthQuery'
import type { ApiError } from '../../types/auth'

interface PasswordStrength {
  score: number
  label: string
  color: 'error' | 'warning' | 'success'
}

function getPasswordStrength(password: string): PasswordStrength {
  let score = 0
  if (password.length >= 8) score++
  if (/[A-Z]/.test(password)) score++
  if (/[a-z]/.test(password)) score++
  if (/[0-9]/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++

  if (score <= 2) return { score, label: 'Débil', color: 'error' }
  if (score <= 4) return { score, label: 'Regular', color: 'warning' }
  return { score, label: 'Fuerte', color: 'success' }
}

function getRegisterErrorMessage(error: AxiosError<ApiError> | null): string | null {
  if (!error) return null
  if (!error.response) return 'Sin conexión. Verifica tu internet'

  const { status, data } = error.response

  if (status === 409) return 'Este email ya está registrado'
  if (status === 400) return data?.message ?? 'Datos inválidos. Verifica la información'

  return 'Ocurrió un error inesperado. Intenta de nuevo'
}

const RegisterPage = () => {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [passwordMismatch, setPasswordMismatch] = useState(false)

  const { mutate: register, isPending, error } = useRegister()

  const strength = getPasswordStrength(password)
  const errorMessage = getRegisterErrorMessage(error)

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (password !== confirmPassword) {
      setPasswordMismatch(true)
      return
    }
    setPasswordMismatch(false)
    register({ fullName, email, password })
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="background.default">
      <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} textAlign="center" mb={1}>
            Crear cuenta
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" mb={3}>
            Registra tu restaurante en QuickStack POS
          </Typography>

          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }} role="alert">
              {errorMessage}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              label="Nombre completo"
              type="text"
              required
              fullWidth
              autoComplete="name"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              sx={{ mb: 2 }}
              inputProps={{ 'aria-label': 'nombre completo' }}
            />

            <TextField
              label="Email"
              type="email"
              required
              fullWidth
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              sx={{ mb: 2 }}
              inputProps={{ 'aria-label': 'email' }}
            />

            <TextField
              label="Contraseña"
              type={showPassword ? 'text' : 'password'}
              required
              fullWidth
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 1 }}
              inputProps={{ 'data-testid': 'password-input' }}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowPassword((prev) => !prev)}
                      edge="end"
                      aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            {password.length > 0 && (
              <Box mb={2}>
                <LinearProgress
                  variant="determinate"
                  value={(strength.score / 5) * 100}
                  color={strength.color}
                  aria-label="fortaleza de contraseña"
                />
                <Typography variant="caption" color={`${strength.color}.main`}>
                  Fortaleza: {strength.label}
                </Typography>
              </Box>
            )}

            <TextField
              label="Confirmar contraseña"
              type={showConfirm ? 'text' : 'password'}
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
              inputProps={{ 'data-testid': 'confirm-password-input' }}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowConfirm((prev) => !prev)}
                      edge="end"
                      aria-label={showConfirm ? 'Ocultar confirmar contraseña' : 'Mostrar confirmar contraseña'}
                    >
                      {showConfirm ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={isPending}
              startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : null}
            >
              {isPending ? 'Creando cuenta...' : 'Crear cuenta'}
            </Button>
          </Box>

          <Box mt={2} textAlign="center">
            <Link component={RouterLink} to="/login" variant="body2">
              ¿Ya tienes cuenta? Inicia sesión
            </Link>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default RegisterPage
