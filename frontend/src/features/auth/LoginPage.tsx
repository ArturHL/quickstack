import { useState } from 'react'
import { Navigate, useSearchParams, useLocation, Link as RouterLink } from 'react-router-dom'
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
import Visibility from '@mui/icons-material/Visibility'
import VisibilityOff from '@mui/icons-material/VisibilityOff'
import { useLogin } from '../../hooks/useAuthQuery'
import { useAuthStore } from '../../stores/authStore'
import type { ApiError } from '../../types/auth'

function getLoginErrorMessage(error: AxiosError<ApiError> | null): string | null {
  if (!error) return null
  if (!error.response) return 'Sin conexión. Verifica tu internet'

  const { status, headers } = error.response

  if (status === 401) return 'Email o contraseña incorrectos'

  if (status === 423) {
    const lockedUntil = headers['x-locked-until'] as string | undefined
    if (lockedUntil) {
      const minutes = Math.ceil((new Date(lockedUntil).getTime() - Date.now()) / 60_000)
      const label = minutes > 0 ? `${minutes} minuto${minutes !== 1 ? 's' : ''}` : 'unos momentos'
      return `Cuenta bloqueada. Intenta de nuevo en ${label}`
    }
    return 'Cuenta bloqueada temporalmente'
  }

  if (status === 429) {
    const retryAfter = headers['retry-after'] as string | undefined
    if (retryAfter) {
      const seconds = Math.ceil(Number(retryAfter))
      return `Demasiados intentos. Espera ${seconds} segundo${seconds !== 1 ? 's' : ''}`
    }
    return 'Demasiados intentos. Espera un momento'
  }

  return 'Ocurrió un error inesperado. Intenta de nuevo'
}

const LoginPage = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const { mutate: login, isPending, error } = useLogin()

  const from = (location.state as { from?: { pathname: string } } | null)?.from?.pathname ?? '/dashboard'
  if (isAuthenticated) return <Navigate to={from} replace />

  const registered = searchParams.get('registered') === 'true'
  const errorMessage = getLoginErrorMessage(error)

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    // TODO: Implement Phase 1 - Get tenantId from user's organization or login flow
    const tenantId = '' // Will be provided by Phase 1 tenant management
    login({ email, password, tenantId })
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="background.default">
      <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} textAlign="center" mb={1}>
            QuickStack POS
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" mb={3}>
            Inicia sesión en tu cuenta
          </Typography>

          {registered && (
            <Alert severity="success" sx={{ mb: 2 }}>
              Cuenta creada. Ya puedes iniciar sesión.
            </Alert>
          )}

          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }} role="alert">
              {errorMessage}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} noValidate>
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
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 3 }}
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

            <Button
              type="submit"
              variant="contained"
              fullWidth
              size="large"
              disabled={isPending}
              startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : null}
            >
              {isPending ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </Button>
          </Box>

          <Box mt={2} textAlign="center">
            <Link component={RouterLink} to="/forgot-password" variant="body2" display="block" mb={0.5}>
              ¿Olvidaste tu contraseña?
            </Link>
            <Link component={RouterLink} to="/register" variant="body2">
              Crear cuenta
            </Link>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default LoginPage
