import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import TextField from '@mui/material/TextField'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import Alert from '@mui/material/Alert'
import CircularProgress from '@mui/material/CircularProgress'
import Link from '@mui/material/Link'
import { useForgotPassword } from '../../hooks/useAuthQuery'

const GENERIC_MESSAGE = 'Si el email existe, recibirás instrucciones en breve'

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { mutate: forgotPassword, isPending } = useForgotPassword()

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    // TODO: Implement Phase 1 - Get tenantId from user's email domain or tenant lookup
    const tenantId = '' // Will be provided by Phase 1 tenant management

    forgotPassword(
      { email, tenantId },
      {
        onSuccess: () => setSubmitted(true),
        onError: () => {
          // Siempre muestra el mismo mensaje — no revelar si el email existe
          setSubmitted(true)
        },
      }
    )
  }

  return (
    <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh" bgcolor="background.default">
      <Card sx={{ width: '100%', maxWidth: 420, mx: 2 }}>
        <CardContent sx={{ p: 4 }}>
          <Typography variant="h5" fontWeight={700} textAlign="center" mb={1}>
            Recuperar contraseña
          </Typography>
          <Typography variant="body2" color="text.secondary" textAlign="center" mb={3}>
            Ingresa tu email y te enviaremos instrucciones
          </Typography>

          {submitted ? (
            <Alert severity="success" sx={{ mb: 3 }}>
              {GENERIC_MESSAGE}
            </Alert>
          ) : (
            <Box component="form" onSubmit={handleSubmit} noValidate>
              <TextField
                label="Email"
                type="email"
                required
                fullWidth
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                sx={{ mb: 3 }}
              />

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={isPending}
                startIcon={isPending ? <CircularProgress size={18} color="inherit" /> : null}
              >
                {isPending ? 'Enviando...' : 'Enviar instrucciones'}
              </Button>
            </Box>
          )}

          <Box mt={2} textAlign="center">
            <Link component={RouterLink} to="/login" variant="body2">
              Volver al login
            </Link>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default ForgotPasswordPage
